package backend;

import keychain.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.json.JsonObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class BackingStore {
    private final static Logger LOGGER = Logger.getLogger(BackingStore.class.getName());

    private static Path getVolume() {
        return Paths.get("./");
    }

    private static Path getKeychainsPath() {
        return getVolume().resolve("keychains");
    }

    private static Path getDirectoryPath() {
        return getVolume().resolve("keychains.directory");
    }

    public static boolean init(SecretKey masterPassword) {
        Path rootPath = getVolume();
        File directory;

        try {
            Files.createDirectories(rootPath);
            directory = Files.createFile(rootPath.resolve("keychains.directory")).toFile();
            Files.createDirectory(getKeychainsPath());
        } catch (IOException e) {
            LOGGER.severe("Directory path is invalid.");
            return false;
        }

        if (directory.exists()) {
            Optional<Directory> readMasterKey = readDirectory(masterPassword, directory.toPath());
            return readMasterKey.isPresent();
        } else {
            if (!writeDirectory(masterPassword, new Directory(getVolume()), directory.toPath())) {
                return false;
            }

            LOGGER.info("Created new encryption key.");
        }

        return true;
    }

    private static Optional<Directory> readDirectory(SecretKey masterPassword, Path directoryFile) {
        Optional<Cipher> unwrapCipher = Crypto.getUnwrapCipher(masterPassword);

        if (!unwrapCipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromCipherStream(directoryFile, unwrapCipher.get());

        if (jsonObject.isPresent()) {
            return Optional.of(new Directory(jsonObject.get(), getVolume()));
        } else {
            return Optional.empty();
        }
    }

    private static boolean writeDirectory(SecretKey masterPassword, Directory directory, Path directoryFile) {
        Optional<Cipher> wrapCipher = Crypto.getWrapCipher(masterPassword);
        if (!wrapCipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return false;
        }

        return Crypto.writeJsonObjectToCipherStream(wrapCipher.get(), directoryFile, directory.dump().build());
    }
}
