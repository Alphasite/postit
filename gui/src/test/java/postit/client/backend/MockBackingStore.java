package postit.client.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by nishadmathur on 2/3/17.
 */
public class MockBackingStore extends BackingStore {
    Path rootDirectory;

    public MockBackingStore(KeyService keyService) throws IOException {
        super(keyService);
        rootDirectory = Files.createTempDirectory("PostItTest");
    }

    public MockBackingStore freshBackingStore() throws IOException {
        MockBackingStore store = new MockBackingStore(keyService);
        store.rootDirectory = rootDirectory;
        return store;
    }

    @Override
    public Path getVolume() {
        return rootDirectory;
    }
}
