package postit.client.backend;

import postit.client.keychain.Container;
import postit.client.keychain.Directory;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.Keychain;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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

            this.directory = new Directory(keyService, this);

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

    private boolean saveContainer() {
        if (container != null) {
            return Crypto.writeJsonObjectToFile(getContainer(), container.dump().build());
        } else {
            return true;
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

        SecretKey key = Crypto.hashedSecretKeyFromBytes(keyService.getMasterKey().getEncoded(), salt.get());

        Optional<JsonObject> object = Crypto.decryptJsonObject(key, nonce, data);

        if (!object.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        this.directory = new Directory(object.get(), keyService, this);
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

        SecretKey key = Crypto.hashedSecretKeyFromBytes(keyService.getMasterKey().getEncoded(), salt.get());

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
