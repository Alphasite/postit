package backend;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.swing.text.html.Option;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class Crypto {
    private final static Logger LOGGER = Logger.getLogger(Crypto.class.getName());

    public static final int GCM_NONCE_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;
    public static final String WRAP_CIPHER = "AESWrap";
    public static final String ENCRYPTION_CIPHER = "AES";
    public static final String GCM_CIPHER = "AES/GCM/NoPadding";
    public static final String DIGEST_ALGORITHM = "SHA-256";

    private static SecureRandom random;
    private static KeyGenerator keyGenerator;
    private static Cipher wrapCipher;
    private static Cipher gcmCipher;

    private static MessageDigest sha;

    public static boolean init() {
        Crypto.removeCryptographyRestrictions();

        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise RNG.");
            return false;
        }

        try {
            keyGenerator = KeyGenerator.getInstance(ENCRYPTION_CIPHER);
            keyGenerator.init(256, random);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AES generator.");
            return false;
        }

        try {
            // TODO Investigate AESWrap vs AES256 + GCM mode
            wrapCipher = Cipher.getInstance(WRAP_CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AESWrap.");
            return false;
        }

        try {
            gcmCipher = Cipher.getInstance(GCM_CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AES GCM.");
            return false;
        }

        try {
            sha = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise SHA256");
            return false;
        }

        return true;
    }

    public static SecretKey generateKey() {
        if (keyGenerator == null) {
            Crypto.init();
        }

        return keyGenerator.generateKey();
    }

    public static Optional<Cipher> getWrapCipher(SecretKey key) {
        try {
            wrapCipher.init(Cipher.WRAP_MODE, key);
        } catch (InvalidKeyException e) {
            LOGGER.warning("Wrap cipher key is invalid.");
            return Optional.empty();
        }

        return Optional.of(wrapCipher);
    }

    public static Optional<Cipher> getUnwrapCipher(SecretKey key) {
        try {
            wrapCipher.init(Cipher.UNWRAP_MODE, key);
        } catch (InvalidKeyException e) {
            LOGGER.warning("Unwrap cipher key is invalid.");
            return Optional.empty();
        }

        return Optional.of(wrapCipher);
    }

    public static Optional<Cipher> getGCMEncryptCipher(SecretKey key, byte[] nonce) {
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        try {
            gcmCipher.init(Cipher.ENCRYPT_MODE, key, spec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            LOGGER.warning("Unable to init GCM encryption cipher.");
            return Optional.empty();
        }

        return Optional.of(gcmCipher);
    }

    public static Optional<Cipher> getGCMDecryptCipher(SecretKey key, byte[] nonce) {
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        try {
            gcmCipher.init(Cipher.DECRYPT_MODE, key, spec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            LOGGER.warning("Unable to init GCM decryption cipher.");
            return Optional.empty();
        }

        return Optional.of(gcmCipher);
    }

    public static byte[] getNonce() {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);
        return nonce;
    }

    public static MessageDigest getSha() {
        return sha;
    }

    public static SecretKey secretKeyFromBytes(byte key[]) {
        // TODO FIX THIS!!!
        return new SecretKeySpec(sha.digest(key), "AES");
    }

    public static byte[] secretKeyToBytes(SecretKey key) {
        return key.getEncoded();
    }

    public static boolean writeJsonObjectToCipherStream(Cipher cipher, Path path, JsonObject jsonObject) {
        try (JsonWriter out = Json.createWriter(new CipherOutputStream(new FileOutputStream(path.toFile()), cipher))) {
            out.writeObject(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error writing to file " + path + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean writeJsonObjectToWrapCipher(Cipher cipher, Path path, JsonObject jsonObject) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter out = Json.createWriter(baos);
        out.writeObject(jsonObject);
        out.close();

        try {
            // Zero pad the key.
            int length = baos.toByteArray().length;
            baos.write(new byte[8 - length % 8]);

            Files.write(path, cipher.wrap(new SecretKeySpec(baos.toByteArray(), "RAW")));
        } catch (IOException e) {
            LOGGER.warning("Error writing file: " + e.getMessage());
            return false;
        } catch (InvalidKeyException | IllegalBlockSizeException e) {
            LOGGER.warning("Error wrapping cipher: " + e.getMessage());
            return false;
        }

        return true;
    }

    public static Optional<JsonObject> readJsonObjectFromCipherStream(Path path, Cipher unwrapCipher) {
        try (JsonReader in = Json.createReader(new CipherInputStream(new FileInputStream(path.toFile()), unwrapCipher))) {

            return Optional.of(in.readObject());

        } catch (IOException e) {
            // TODO figure this out?
            LOGGER.severe("Invalid password?");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<JsonObject> readJsonObjectFromWrapCipher(Path path, Cipher cipher) {
        try {
            byte[] bytes = Files.readAllBytes(path);

            bytes = cipher.unwrap(bytes, WRAP_CIPHER, Cipher.SECRET_KEY).getEncoded();

            return Optional.of(Json.createReader(new ByteArrayInputStream(bytes)).readObject());

        } catch (IOException e) {
            // TODO figure this out?
            LOGGER.severe("Error reading file: " + e.getMessage());
            return Optional.empty();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.severe("Error decrypting file: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * A method of disabling the java crypto strength restrictions.
     *
     * Installing the policy file isn't practical for user code. So this approach is better
     *
     * Source: http://stackoverflow.com/questions/1179672/
     */
    private static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            LOGGER.fine("Cryptography restrictions removal not needed");
            return;
        }
        try {
        /*
         * Do the following, but with reflection to bypass access checks:
         *
         * JceSecurity.isRestricted = false;
         * JceSecurity.defaultPolicy.perms.clear();
         * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
         */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

            LOGGER.fine("Successfully removed cryptography restrictions");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove cryptography restrictions", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        // This simply matches the Oracle JRE, but not OpenJDK.
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }
}
