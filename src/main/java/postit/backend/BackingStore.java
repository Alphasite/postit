package postit.backend;

import postit.keychain.Directory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by nishadmathur on 28/2/17.
 */
public interface BackingStore {
    Path getVolume();

    Path getKeychainsPath();

    Path getDirectoryPath();

    boolean init();

    Optional<Directory> readDirectory();

    boolean writeDirectory(Directory directory);
}
