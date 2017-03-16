package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;
import postit.client.backend.MockKeyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

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
        keyService = new MockKeyService(Crypto.secretKeyFromBytes("test".getBytes()), null);
        backingStore = new BackingStore(keyService);
        directory = new Directory(keyService, backingStore);

        key = Crypto.generateKey();
        entry = new DirectoryEntry("test", key, directory, keyService, backingStore);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void dump() throws Exception {

    }

    @Test
    public void resolve() throws Exception {

    }

}