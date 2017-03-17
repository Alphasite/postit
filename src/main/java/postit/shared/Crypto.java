package postit.shared;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Crypto {
    private final static Logger LOGGER = Logger.getLogger(Crypto.class.getName());

    private static final int GCM_NONCE_LENGTH = 12;
    private static final String ENCRYPTION_CIPHER = "AES";

    private static final int CPU_SCALING_FACTOR = 10;
    private static final int MEMORY_SCALING_FACTOR = 10;
    private static final int PARALLELISM_SCALING_FACTOR = 10;
    private static final int KEY_LENGTH = 32;

    private static SecureRandom random;
    private static KeyGenerator keyGenerator;

    public static boolean init() {
        return init(true);
    }

    public static boolean init(boolean useSecureRandom) {
        Crypto.removeCryptographyRestrictions();
        Security.addProvider(new BouncyCastleProvider());

        try {
            if (useSecureRandom) {
                random = SecureRandom.getInstanceStrong();
            } else {
                random = new SecureRandom();
            }
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

        return true;
    }

    public static SecretKey generateKey() {
        if (keyGenerator == null) {
            Crypto.init();
        }

        return keyGenerator.generateKey();
    }

    public static byte[] getNonce() {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);
        return nonce;
    }

    public static SecretKey hashedSecretKeyFromBytes(byte[] key, byte[] salt) {
        return Crypto.secretKeyFromBytes(SCrypt.generate(
                key,
                salt,
                CPU_SCALING_FACTOR,
                MEMORY_SCALING_FACTOR,
                PARALLELISM_SCALING_FACTOR,
                KEY_LENGTH
        ));
    }

    public static SecretKey secretKeyFromBytes(byte key[]) {
        // TODO FIX THIS!!!
        return new SecretKeySpec(key, "AES");
    }

    public static byte[] secretKeyToBytes(SecretKey key) {
        return key.getEncoded();
    }

    public static Optional<byte[]> encryptJsonObject(SecretKey key, byte[] nonce, JsonObject object) {
        AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        KeyParameter keyParameter = new KeyParameter(key.getEncoded());
        AEADParameters parameters = new AEADParameters(keyParameter, 128, nonce);

        try {
            cipher.init(true, parameters);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Failed to initialise cipher: " + e.getMessage());
            return Optional.empty();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (JsonWriter out = Json.createWriter(new CipherOutputStream(baos, cipher))) {
            out.write(object);
        } catch (JsonException | IllegalStateException e) {
            LOGGER.warning("Couldn't write json object, error in underlying streams: " + e.getMessage());
            return Optional.empty();
        }

        return Optional.of(baos.toByteArray());
    }

    public static Optional<JsonObject> decryptJsonObject(SecretKey key, byte[] nonce, byte[] bytes) {
        AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        KeyParameter keyParameter = new KeyParameter(key.getEncoded());
        AEADParameters parameters = new AEADParameters(keyParameter, 128, nonce);

        try {
            cipher.init(false, parameters);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Failed to initialise cipher: " + e.getMessage());
            return Optional.empty();
        }

        try (JsonReader out = Json.createReader(new CipherInputStream(new ByteArrayInputStream(bytes), cipher))) {
            return Optional.of(out.readObject());
        } catch (JsonParsingException e) {
            LOGGER.warning("Mac Check Failed or Malformed json for object: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        } catch (JsonException | IllegalStateException e) {
            LOGGER.warning("Couldn't read json object, error in underlying streams: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean writeJsonObjectToFile(Path path, JsonObject jsonObject) {
        try (JsonWriter out = Json.createWriter(new FileOutputStream(path.toFile()))) {
            out.writeObject(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error writing json object to file [" + path + "]: " + e.getMessage());
            return false;
        }

        return true;
    }

    public static Optional<JsonObject> readJsonObjectFromFile(Path path) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try (JsonReader in = Json.createReader(new FileInputStream(path.toFile()))) {
            return Optional.of(in.readObject());
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error reading json object from file [" + path + "]: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * A method of disabling the java crypto strength restrictions.
     * <p>
     * Installing the policy file isn't practical for user code. So this approach is better
     * <p>
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
