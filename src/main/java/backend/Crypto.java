package backend;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
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

    private static SecureRandom random;
    private static KeyGenerator keyGenerator;
    private static Cipher wrapCipher;
    private static Cipher gcmCipher;

    public static boolean init() {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise RNG.");
            return false;
        }

        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, random);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AES generator.");
            return false;
        }

        try {
            // TODO Investigate AESWrap vs AES256 + GCM mode
            wrapCipher = Cipher.getInstance("AESWrap");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AESWrap.");
            return false;
        }

        try {
            gcmCipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise AES GCM.");
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

    public static SecretKey secretKeyFromBytes(byte key[]) {
        return new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
    }

    public static byte[] secretKeyToBytes(SecretKey key) {
        return Base64.getEncoder().encode(key.getEncoded());
    }

    public static boolean writeJsonObjectToCipherStream(Cipher cipher, Path path, JsonObject jsonObject) {
        try (JsonWriter out = Json.createWriter(new CipherOutputStream(new FileOutputStream(path.toFile()), cipher))) {
            if (!path.toFile().createNewFile()) {
                LOGGER.severe("Error creating wrapped key file.");
                return false;
            }

            out.writeObject(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
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
}
