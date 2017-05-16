package postit.client.backend;

import postit.client.keychain.*;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class BackingStore {
    private final static Logger LOGGER = Logger.getLogger(BackingStore.class.getName());

    KeyService keyService;

    Container container;

    Directory directory;

    public BackingStore(KeyService keyService) {
        this.keyService = keyService;
        this.container = null;
        this.directory = null;
    }

    public Path getVolume() {
        return Paths.get("./");
    }

    public Path getContainer() {
        return getVolume().resolve("container.json");
    }

    private Path getKeyPairPath() {
        return getVolume().resolve("keypair");
    }

    private Path getPublicKeyPath() {
        return getVolume().resolve("rsa_key.pub");
    }

    public boolean init() {
        Path rootPath = getVolume();

        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }

        } catch (IOException e) {
            LOGGER.severe("Errors creating directories.");
            return false;
        }

        if (!Files.exists(rootPath.resolve(getContainer()))) {
            container = new Container();
            container.salt = Base64.getEncoder().encodeToString(Crypto.getNonce());

            this.directory = new Directory(this, keyService);

            if (writeDirectory()) {
                LOGGER.info("Created container.");
            } else {
                LOGGER.info("Error creating container.");
                return false;
            }
        }

        return save();
    }

    private Optional<Container> loadContainer() {
        // Return the container if its already been loaded.
        if (container != null) {
            return Optional.of(container);
        }

        // Otherwise load it from disk.
        // TODO validation
        Optional<JsonObject> object = Crypto.readJsonObjectFromFile(getContainer());

        if (object.isPresent()) {
            container = new Container(object.get());
            return Optional.of(container);
        } else {
            return Optional.empty();
        }
    }

    public boolean saveContainer() {
        if (container != null) {
            return Crypto.writeJsonObjectToFile(getContainer(), container.dump().build());
        } else {
            return true;
        }
    }

    public boolean writeKeypair(Account account) {
        if (Crypto.writeJsonObjectToFile(getKeyPairPath(), account.dumpKeypairs(keyService.getMasterKey(false)).get().build())) {
            return writePublicKeys(account);
        } else {
            LOGGER.warning("Failed to save keypair.");
            return false;
        }
    }

    public boolean writePublicKeys(Account account) {
        try (PrintWriter writer = new PrintWriter(getPublicKeyPath().toFile(),"UTF-8")) {

            Crypto.writeJsonObjectToFile(getKeyPairPath(), account.dumpKeypairs(keyService.getMasterKey(true)).get().build());
            Files.write(getPublicKeyPath(), Crypto.serialiseObject(account.getEncryptionKeypair().getPublic()).getBytes("UTF-8"));
            RSAPublicKey encryptionKey = (RSAPublicKey) account.getEncryptionKeypair().getPublic();
            RSAPublicKey signingKey = (RSAPublicKey) account.getSigningKeypair().getPublic();

            X509EncodedKeySpec x509EncodedEncryptionKeySpec = new X509EncodedKeySpec(encryptionKey.getEncoded());
            X509EncodedKeySpec x509EncodedSingingKeySpec = new X509EncodedKeySpec(signingKey.getEncoded());

            Base64.Encoder encoder = Base64.getEncoder();
            writer.println(encoder.encodeToString(x509EncodedEncryptionKeySpec.getEncoded()));
            writer.println(encoder.encodeToString(x509EncodedSingingKeySpec.getEncoded()));

            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to save keypair... " + e.getMessage());
            return false;
        }
    }

    public boolean readKeypair(Account account) {
        Optional<JsonObject> object = Crypto.readJsonObjectFromFile(getKeyPairPath());

        if (object.isPresent()) {
            account.deserialiseKeypairs(keyService.getMasterKey(false), object.get());
            return true;
        } else {
            return false;
        }
    }

    public Optional<List<RSAPublicKey>> readPublicKey(Path path) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            List<String> keyLines = Files.readAllLines(path);

            X509EncodedKeySpec spec;
            KeyFactory kf = KeyFactory.getInstance("RSA");

            spec = new X509EncodedKeySpec(decoder.decode(keyLines.get(0)));
            RSAPublicKey encryptionKey = (RSAPublicKey) kf.generatePublic(spec);

            spec = new X509EncodedKeySpec(decoder.decode(keyLines.get(1)));
            RSAPublicKey signingKey = (RSAPublicKey) kf.generatePublic(spec);

            ArrayList<RSAPublicKey> keys = new ArrayList<>();
            keys.add(encryptionKey);
            keys.add(signingKey);

            return Optional.of(keys);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<byte[]> getSalt() {
        Optional<Container> optionalContainer = loadContainer();

        if (!optionalContainer.isPresent()) {
            // TODO
            return Optional.empty();
        }

        return Optional.of(Base64.getDecoder().decode(container.salt));
    }

    public Optional<Directory> readDirectory() {
        if (directory != null) {
            return Optional.of(directory);
        }

        Optional<Container> optionalContainer = loadContainer();
        Optional<byte[]> salt = getSalt();

        if (!optionalContainer.isPresent() || !salt.isPresent()) {
            // TODO
            return Optional.empty();
        }

        Container container = optionalContainer.get();

        Base64.Decoder decoder = Base64.getDecoder();

        byte[] nonce = decoder.decode(container.directoryNonce);
        byte[] data = decoder.decode(container.directory);

        SecretKey key = Crypto.hashedSecretKeyFromBytes(keyService.getMasterKey(false).getEncoded(), salt.get());

        Optional<JsonObject> object = Crypto.decryptJsonObject(key, nonce, data);

        if (!object.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        this.directory = new Directory(object.get(), this);
        return Optional.of(directory);
    }

    public boolean writeDirectory() {
        if (directory == null) {
            return true;
        }

        Base64.Encoder encoder = Base64.getEncoder();

        Optional<Container> optionalContainer = loadContainer();
        Optional<byte[]> salt = getSalt();

        if (!optionalContainer.isPresent() || !salt.isPresent()) {
            // TODO
            return false;
        }

        byte[] nonce = Crypto.getNonce();

        SecretKey key = Crypto.hashedSecretKeyFromBytes(keyService.getMasterKey(false).getEncoded(), salt.get());

        Optional<byte[]> bytes = Crypto.encryptJsonObject(key, nonce, directory.dump().build());

        if (!bytes.isPresent()) {
            // TODO
            return false;
        }

        container.directoryNonce = encoder.encodeToString(nonce);
        container.directory = encoder.encodeToString(bytes.get());

        return true;
    }

    public Optional<Keychain> readKeychain(DirectoryEntry entry) {
        LOGGER.info("Reading keychain " + entry.name);

        Optional<Container> container = loadContainer();

        if (container.isPresent() && container.get().keychains.containsKey(entry.name)) {

            String backingStore = container.get().keychains.get(entry.name);

            System.out.println("Loading   key: " + Base64.getEncoder().encodeToString(entry.getEncryptionKey().getEncoded()));
            System.out.println("Loading nonce: " + Base64.getEncoder().encodeToString(entry.getNonce()));
            System.out.println("Loading  data: " + backingStore);


            // otherwise load and decrypt it.
            Optional<JsonObject> object = Crypto.decryptJsonObject(
                    entry.getEncryptionKey(),
                    entry.getNonce(),
                    Base64.getDecoder().decode(backingStore)
            );

            if (object.isPresent()) {
                LOGGER.info("Succeeded in reading keychain " + entry.name + " from disk.");
                return Optional.of(new Keychain(object.get(), entry));
            } else {
                return Optional.empty();
            }
        } else {
            LOGGER.info("Keychain file doesn't exist... creating it: " + entry.name);

            return Optional.of(new Keychain(entry));
        }
    }

    public boolean writeKeychain(DirectoryEntry entry) {
        Base64.Encoder encoder = Base64.getEncoder();

        Optional<Container> container = loadContainer();

        Optional<Keychain> keychain = entry.readKeychain();
        if (keychain.isPresent()) {

            entry.setEncryptionKey(Crypto.generateKey());
            entry.setNonce(Crypto.getNonce());

            Optional<byte[]> bytes = Crypto.encryptJsonObject(
                    entry.getEncryptionKey(),
                    entry.getNonce(),
                    keychain.get().dump().build()
            );

            if (!bytes.isPresent()) {
                LOGGER.warning("Failed to encrypt keychain :" + entry.name);
                return false;
            }

            if (container.isPresent()) {
                container.get().keychains.put(entry.name, encoder.encodeToString(bytes.get()));
                return true;
            }
        }

        return false;
    }

    public boolean deleteKeychain(String name) {
        Optional<Container> container = this.loadContainer();

        if (!container.isPresent()) {
            return false;
        }

        container.get().keychains.remove(name);
        return true;
    }

    public boolean save() {

        Optional<Directory> directory = readDirectory();

        if (!directory.isPresent()) {
            LOGGER.warning("Failed to load directory.");
            return false;
        }

        for (DirectoryEntry entry : directory.get().getKeychains()) {
            writeKeychain(entry);
        }

        this.writeDirectory();

        return saveContainer();
    }
}
