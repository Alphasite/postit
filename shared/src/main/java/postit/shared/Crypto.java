package postit.shared;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.*;
import javax.json.stream.JsonParsingException;
import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Crypto {
    private final static Logger LOGGER = Logger.getLogger(Crypto.class.getName());

    private static final int GCM_NONCE_LENGTH = 12;
    private static final String ENCRYPTION_CIPHER = "AES";

    private static final int CPU_SCALING_FACTOR = (int) Math.pow(2,20);
    private static final int MEMORY_SCALING_FACTOR = 8;
    private static final int PARALLELISM_SCALING_FACTOR = 1;
    private static final int KEY_LENGTH = 32;

    private static SecureRandom random;
    private static KeyGenerator keyGenerator;
    private static SSLContext sslContext;

    public static boolean init() {
        return init(true);
    }

    public static boolean init(boolean useSecureRandom) {
        Crypto.removeCryptographyRestrictions();
        Security.addProvider(new BouncyCastleProvider());

        URL keyPath = Crypto.class.getClassLoader().getResource("keys/test-certificate.jks");// TODO unfix this.
        if (keyPath == null) {
            LOGGER.severe("FAILED TO LOAD CERTIFICATE.");
            return false;
        }

        System.setProperty("javax.net.ssl.keyStore", keyPath.getPath());
        System.setProperty("javax.net.ssl.trustStore", keyPath.getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

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

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = Crypto.class.getClassLoader().getResourceAsStream("keys/test-certificate.jks");
            keystore.load(keystoreStream, "password".toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, "password".toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, random);
        } catch (KeyStoreException | KeyManagementException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
            LOGGER.severe("Failed to create SSL context: " + e.getMessage());
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

    public static Optional<byte[]> encryptJsonObject(Key key, byte[] nonce, JsonObject object) {
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

    public static Optional<JsonObject> decryptJsonObject(Key key, byte[] nonce, byte[] bytes) {
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

    public static Optional<KeyPair> generateRSAKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096, random);
            KeyPair pair = generator.generateKeyPair();
            return Optional.of(pair);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("FAILED TO LOAD RSA: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<byte[]> wrapKey(Key key, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            // encrypt the plaintext using the public key
            cipher.init(Cipher.WRAP_MODE, publicKey);
            return Optional.of(cipher.wrap(key));
        } catch (Exception e) {
            LOGGER.severe("Failed to encrypt data using public key: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Key> unwrapKey(byte[] key, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            // encrypt the plaintext using the public key
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            return Optional.of(cipher.unwrap(key, "AES", Cipher.SECRET_KEY));
        } catch (Exception e) {
            LOGGER.severe("Failed to encrypt data using public key: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static String serialiseKeypair(KeyPair keyPair) {
        String keypair;
        try (
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream o =  new ObjectOutputStream(b)
        ) {
            o.writeObject(keyPair);
            byte[] res = b.toByteArray();
            keypair = Base64.getEncoder().encodeToString(res);
        } catch (IOException e) {
            LOGGER.severe("HIT ERROR STATE FAILED TO SERIALIZE key pair: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return keypair;
    }

    public static KeyPair deserialiseKeypair(String keypair) {
        try {
            try (
                    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(keypair));
                    ObjectInputStream oi = new ObjectInputStream(bais)
            ) {
                return (KeyPair) oi.readObject();
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.severe("HIT ERROR STATE FAILED TO DESERIALIZE key pair: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static SSLContext getSSLContext() {
        return sslContext;
    }

    public static SecureRandom getRandom() {
        return random;
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
