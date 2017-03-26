package postit.client.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import postit.client.backend.MockBackingStore;
import postit.client.backend.MockKeyService;
import postit.client.keychain.Account;
import postit.client.keychain.Directory;
import postit.shared.Crypto;
import postit.shared.communication.Client;

import java.nio.file.Files;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nishadmathur on 8/3/17.
 */
public class ServerControllerTest {
    private final static Logger LOGGER = Logger.getLogger(ServerControllerTest.class.getName());

    ServerController serverController;

    Client clientToServer;
    MockKeyService keyService;
    MockBackingStore backingStore;
    Directory directory;

    DirectoryController directoryController;

    @Before
    public void setUp() throws Exception {
        LOGGER.info("----Setup");
        System.err.println("Begin");

        try {
            Crypto.init(false);

            keyService = new MockKeyService(Crypto.secretKeyFromBytes("ServerController".getBytes()), null);
            keyService.account = new Account("test", "password");

            backingStore = new MockBackingStore(keyService);
            backingStore.init();

            directory = backingStore.readDirectory().get();
            directoryController = new DirectoryController(directory, backingStore, keyService);
        } catch (Exception e) {
            Files.deleteIfExists(backingStore.getContainer());
            throw e;
        }
        LOGGER.info(backingStore.getVolume().toString());

        clientToServer = new Client(2048, "localhost");

        serverController = new ServerController(clientToServer);
        serverController.setDirectoryController(directoryController);

    }

    @After
    public void tearDown() throws Exception {
        LOGGER.info("----Tear down");

        Files.deleteIfExists(backingStore.getContainer());
        Files.deleteIfExists(backingStore.getVolume());
    }

    @Test
    public void sync() throws Exception {
        LOGGER.info("----sync");

    }

    @Test
    public void addUser() throws Exception {
        LOGGER.info("----addUser");

        assertThat(serverController.addUser(directoryController.getAccount(),
                                            "test@addUser.com",
                                            "TestAddUser",
                                            "TestAddUser"),
                is(true));

    }

    @Test
    public void authenticate() throws Exception {
        LOGGER.info("----authenticate");
    }

    @Test
    public void getKeychains() throws Exception {
        LOGGER.info("----getKeychains");
    }

    @Test
    public void getDirectoryKeychainObject() throws Exception {
        LOGGER.info("----getDirectoryKeychainObject");
    }

    @Test
    public void createKeychain() throws Exception {
        LOGGER.info("----createKeychain");
    }

    @Test
    public void setKeychain() throws Exception {
        LOGGER.info("----setKeychain");
    }

    @Test
    public void deleteKeychain() throws Exception {
        LOGGER.info("----deleteKeychain");
    }
}