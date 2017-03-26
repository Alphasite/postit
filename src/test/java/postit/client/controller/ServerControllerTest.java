package postit.client.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import postit.client.backend.MockBackingStore;
import postit.client.backend.MockKeyService;
import postit.client.keychain.Account;
import postit.client.keychain.Directory;
import postit.client.keychain.Keychain;
import postit.shared.Crypto;
import postit.shared.communication.Client;

import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.Assert.*;

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

            keyService = new MockKeyService(Crypto.secretKeyFromBytes("DirectoryControllerTest".getBytes()), null);
            keyService.account = new Account("test", "password");

            backingStore = new MockBackingStore(keyService);
            backingStore.init();

            directory = backingStore.readDirectory().get();
            directoryController = new DirectoryController(directory, backingStore, keyService);
        } catch (Exception e) {
            Files.deleteIfExists(backingStore.getContainer());
            throw e;
        }


        clientToServer = new Client(2048, "localhost");
        serverController = new ServerController(clientToServer);
        assertTrue(serverController.setDirectoryController(directoryController));

    }

    @After
    public void tearDown() throws Exception {
        LOGGER.info("----Tear down");

        Files.deleteIfExists(backingStore.getContainer());
        Files.deleteIfExists(backingStore.getVolume());
    }

    @Test
    public void testSync() throws Exception {
        LOGGER.info("----sync");

    }

    @Test
    public void runTestSeries() throws Exception{
        addUser("testServerController","password","test@servercontroller.com","test","server");
        assertTrue(authenticate("testServerController","password"));
        assertFalse(authenticate("NOTtestServerController","password"));
        assertFalse(authenticate("testServerController","NOTpassword"));

        createKeychain();
        setKeychain();
        getKeychains();
        deleteKeychain();
    }

    public void addUser(String username, String password, String email, String firstname, String lastname) throws Exception {
        LOGGER.info("----addUser");

        Account testAccount = new Account(username,password);
        assertTrue(serverController.addUser(testAccount, email, firstname,lastname));
        assertFalse(serverController.addUser(testAccount, email, firstname,lastname));
    }

    public boolean authenticate(String username, String password) throws Exception {
        LOGGER.info("----authenticate");

        return serverController.authenticate(new Account(username, password));

    }


    public void createKeychain() throws Exception {
        LOGGER.info("----createKeychain");

        directoryController.createKeychain("testServerController1");
        assertTrue(serverController.createKeychain(directoryController.getKeychains().get(0)));
    }

    public void setKeychain() throws Exception {
        LOGGER.info("----setKeychain");
        Keychain keychain = directoryController.getKeychain("testServerController1").get();
        directoryController.createPassword(keychain, "password1", Crypto.secretKeyFromBytes("secret1".getBytes()));
        serverController.setKeychain(directoryController.getKeychains().get(0));
    }

    public void getKeychains() throws Exception {
        LOGGER.info("----getKeychains");
        for(int i=0; i<directoryController.getKeychains().size();i++){
            Long server_serverID = serverController.getKeychains().get(i);
            Long directory_serverID = directoryController.getKeychains().get(i).serverid;
            assertEquals(server_serverID,directory_serverID);
        }

    }

    public void deleteKeychain() throws Exception {
        LOGGER.info("----deleteKeychain");
        assertTrue(serverController.deleteKeychain(directoryController.getKeychains().get(0).serverid));
    }


}