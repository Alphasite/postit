package postit.backend;

import postit.keychain.Directory;

import javax.crypto.Cipher;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class BackingStoreImpl implements BackingStore {
    private final static Logger LOGGER = Logger.getLogger(BackingStoreImpl.class.getName());

    KeyService keyService;

    public BackingStoreImpl(KeyService keyService) {
        this.keyService = keyService;
    }

    @Override
    public Path getVolume() {
        return Paths.get("./");
    }

    @Override
    public Path getKeychainsPath() {
        return getVolume().resolve("keychains");
    }

    @Override
    public Path getDirectoryPath() {
        return getVolume().resolve("keychains.directory");
    }

    @Override
    public boolean init() {
        Path rootPath = getVolume();

        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }

            if (!Files.exists(getKeychainsPath())) {
                Files.createDirectory(getKeychainsPath());
            }

        } catch (IOException e) {
            LOGGER.severe("Errors creating directories.");
            return false;
        }

        if (!Files.exists(rootPath.resolve(getDirectoryPath()))) {
            boolean created = writeDirectory(new Directory(keyService, this));

            if (created) {
                LOGGER.info("Created new encryption key.");
            } else {
                LOGGER.info("Error creating directory.");
                return false;
            }
        }

        return true;
    }

    @Override
    public Optional<Directory> readDirectory() {
        Optional<Cipher> unwrapCipher = Crypto.getUnwrapCipher(keyService.getMasterKey());

        if (!unwrapCipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromWrapCipher(getDirectoryPath(), unwrapCipher.get());

        if (jsonObject.isPresent()) {
            return Optional.of(new Directory(jsonObject.get(), keyService, this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean writeDirectory(Directory directory) {
        Optional<Cipher> wrapCipher = Crypto.getWrapCipher(keyService.getMasterKey());
        if (!wrapCipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return false;
        }

        return Crypto.writeJsonObjectToWrapCipher(wrapCipher.get(), getDirectoryPath(), directory.dump().build());
    }
}
