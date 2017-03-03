package postit.keychain;

import postit.backend.BackingStore;
import postit.backend.BackingStoreImpl;
import postit.backend.Crypto;
import postit.backend.MockKeyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.nio.file.Files;

/**
 * Created by nishadmathur on 24/2/17.
 */
public class DirectoryEntryTest {
    DirectoryEntry entry;
    SecretKey key;

    MockKeyService keyService;
    BackingStore backingStore;
    Directory directory;

    @Before
    public void setUp() throws Exception {
        keyService = new MockKeyService(Crypto.secretKeyFromBytes("test".getBytes()));
        backingStore = new BackingStoreImpl(keyService);
        directory = new Directory(keyService, backingStore);

        key = Crypto.generateKey();
        entry = new DirectoryEntry("test", key, directory, keyService, backingStore);
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(backingStore.getDirectoryPath());
        Files.deleteIfExists(backingStore.getKeychainsPath());
    }

    @Test
    public void dump() throws Exception {

    }

    @Test
    public void resolve() throws Exception {

    }

}