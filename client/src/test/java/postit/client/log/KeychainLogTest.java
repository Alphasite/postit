package postit.client.log;

import org.junit.Before;
import org.junit.Test;
import postit.client.backend.MockBackingStore;
import postit.client.backend.MockKeyService;
import postit.client.controller.DirectoryController;
import postit.client.controller.DirectoryControllerTest;
import postit.client.keychain.*;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nishadmathur on 15/5/17.
 */
@SuppressWarnings("Duplicates")
public class KeychainLogTest {
    private final static Logger LOGGER = Logger.getLogger(DirectoryControllerTest.class.getName());

    DirectoryEntry entry;
    SecretKey key;

    MockKeyService keyService;
    MockBackingStore backingStore;
    Directory directory;

    DirectoryController controller;

    Account account;
    Share ownerShare;
    RSAPublicKey encryptionKey;
    RSAPublicKey signgingKey;

    KeychainLog kl;

    @Before
    public void setUp() throws Exception {
        LOGGER.info("----Setup");

        try {
            Crypto.init(false);

            keyService = new MockKeyService(Crypto.secretKeyFromBytes("DirectoryControllerTest".getBytes()), null);
            keyService.account = new Account("test", "password");

            backingStore = new MockBackingStore(keyService);
            backingStore.init();

            directory = backingStore.readDirectory().get();
            controller = new DirectoryController(directory, backingStore, keyService);

            account = keyService.getAccount();
            encryptionKey = (RSAPublicKey) account.getEncryptionKeypair().getPublic();
            signgingKey = (RSAPublicKey) account.getSigningKeypair().getPublic();
            ownerShare = new Share(-1L, account.getUsername(), true, encryptionKey, signgingKey, true);

            kl = new KeychainLog();

            KeychainLog.KEYCHAIN_LOG = backingStore.getVolume() + "/log";
            System.out.println("Log dir:" + kl.KEYCHAIN_LOG);

        } catch (Exception e) {
            Files.deleteIfExists(backingStore.getContainer());
            throw e;
        }

        LOGGER.info(backingStore.getVolume().toString());
    }

    @Test
    public void addCreateKeychainLogEntry() throws Exception {
        controller.createKeychain("test");
        DirectoryEntry entry = controller.getKeychains().get(0);

        kl.addCreateKeychainLogEntry(entry, "ning", true, 1, "added keychain keychain1");
        kl.addCreateKeychainLogEntry(entry, "ning", false, 2, "added keychain2");
        kl.addCreateKeychainLogEntry(entry, "ning", true, 3, "added keychain3");
        kl.addUpdateKeychainLogEntry(entry, "ning", true, 1, "updated keychain1");
        kl.printLog(kl.getKeychainLogEntries(entry));

        assertThat(kl.getKeychainLogEntries(entry).size(), is(3));

        kl.dumpLogs(controller);
        assertThat(Files.exists(Paths.get(KeychainLog.KEYCHAIN_LOG)), is(true));

        kl.addCreateKeychainLogEntry(entry, "ning", true, 4, "added keychain keychain1");
        kl.addCreateShareLogEntry(entry, "ning", true, 1, "added keychain keychain1");
        kl.addRemoveKeychainLogEntry(entry, "ning", true, 1, "added keychain keychain1");
        kl.addRemoveShareLogEntry(entry, "ning", true, 1, "added keychain keychain1");
        kl.addUpdateKeychainLogEntry(entry, "ning", true, 1, "added keychain keychain1");
        kl.addUpdateShareLogEntry(entry, "ning", true, 1, "added keychain keychain1");

        assertThat(kl.getKeychainLogEntries(entry).size(), is(9));

        kl.dumpLogs(controller);
        assertThat(Files.exists(Paths.get(KeychainLog.KEYCHAIN_LOG)), is(true));
    }

}