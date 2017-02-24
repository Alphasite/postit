package Backend;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

    private static SecureRandom random;
    private static KeyGenerator keyGenerator;
    private static Cipher wrapCipher;

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

    public static SecretKey secretKeyFromBytes(byte key[]) {
        return new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
    }

    public static byte[] secretKeyToBytes(SecretKey key) {
        return Base64.getEncoder().encode(key.getEncoded());
    }
}
