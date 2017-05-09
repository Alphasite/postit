package postit.client.controller;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import postit.client.backend.MockBackingStore;
import postit.client.backend.MockKeyService;
import postit.client.communication.Client;
import postit.client.keychain.*;
import postit.server.database.Database;
import postit.server.database.TestH2;
import postit.server.netty.RequestInitializer;
import postit.shared.Crypto;

import javax.net.ssl.SSLContext;
import java.nio.file.Files;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created by nishadmathur on 8/3/17.
 */
@SuppressWarnings("Duplicates")
public class ServerControllerTest {
    private final static Logger LOGGER = Logger.getLogger(ServerControllerTest.class.getName());

    private ServerController serverController;

    Account account;

    private Client clientToServer;
    private MockKeyService keyService;
    private MockBackingStore backingStore;
    private Directory directory;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private DirectoryController directoryController;

    @Before
    public void setUp() throws Exception {
        LOGGER.info("----Setup");
        System.err.println("Begin");

        Crypto.init(false);

        Database database;

        database = new TestH2();
        database.initDatabase();

        SSLContext ctx = Crypto.getSSLContext();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new RequestInitializer(ctx, database));

        System.out.println("Server almost ready...");

        ChannelFuture channel = b.bind(2048).sync();
        assertThat(channel.cause(), nullValue());

        account = new Account("test", "password");

        keyService = new MockKeyService(Crypto.secretKeyFromBytes("DirectoryControllerTest".getBytes()), null);
        keyService.account = account;

        backingStore = new MockBackingStore(keyService);
        backingStore.init();

        directory = backingStore.readDirectory().get();
        directoryController = new DirectoryController(directory, backingStore, keyService);

        clientToServer = new Client(2048, "localhost");
        serverController = new ServerController(clientToServer);
        assertTrue(serverController.setDirectoryController(directoryController));

        serverController.addUser(account, "test@test.com", "te", "st", "8000000000", "");
    }

    @After
    public void tearDown() throws Exception {
        LOGGER.info("----Tear down");

        bossGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).sync();
        workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).sync();

        Files.deleteIfExists(backingStore.getContainer());
        Files.deleteIfExists(backingStore.getVolume());
    }

    @Test
    public void runTestSeries() throws Exception {
        Account user1 = addUser("testServerController", "password", "test@servercontroller.com", "test", "server", "8000000000");
        Account user2 = addUser("testServerController2", "password", "test2@servercontroller.com", "test", "server", "8000000000");
        assertTrue(authenticate("testServerController", "password"));
        assertFalse(authenticate("NOTtestServerController", "password"));
        assertFalse(authenticate("testServerController", "NOTpassword"));


        createKeychain();
        setKeychain();
        sync();

        getKeychains();
        deleteKeychain();

        assertThat(directoryController.createKeychain("test7"), is(true));

        sync();

        List<DirectoryEntry> keychains = directoryController.getKeychains();
        DirectoryEntry entry = keychains.stream().filter(e -> e.name.equals("test7")).findAny().get();

        assertThat(directoryController.shareKeychain(entry, new Share(
                -1,
                user2.getUsername(),
                true,
                (RSAPublicKey) user2.getEncryptionKeypair().getPublic(),
                false
        )), is(true));

        assertThat(directoryController.shareKeychain(entry, new Share(
                -1,
                "fake user",
                true,
                (RSAPublicKey) user2.getEncryptionKeypair().getPublic(),
                false
        )), is(true));

        assertThat(entry.shares.size(), is(3));

        sync();

        assertThat(entry.shares.size(), is(2));
        long serverid1 = entry.shares.get(0).serverid;
        long serverid2 = entry.shares.get(1).serverid;
        assertThat(serverid1, not(is(-1)));
        assertThat(serverid2, not(is(-1)));
        Optional<DirectoryKeychain> directoryKeychainObject1 = serverController.getDirectoryKeychainObject(account, serverid1);
        Optional<DirectoryKeychain> directoryKeychainObject2 = serverController.getDirectoryKeychainObject(account, serverid2);
        assertThat(directoryKeychainObject1.isPresent(), is(true));
        assertThat(directoryKeychainObject2.isPresent(), is(true));
        assertThat(directoryKeychainObject1.get().serverid, anyOf(is(serverid1), is(serverid2)));
        assertThat(directoryKeychainObject2.get().serverid, anyOf(is(serverid1), is(serverid2)));
    }

    private void sync() throws InterruptedException {
        TestDataContainer testDataContainer = new TestDataContainer();

        synchronized (testDataContainer) {
            testDataContainer.success = false;

            serverController.sync(() -> {
                synchronized (testDataContainer) {
                    testDataContainer.success = true;
                    testDataContainer.notifyAll();
                }
            });

            testDataContainer.wait(10000);
            assertThat(testDataContainer.success, is(true));
        }
    }

    public Account addUser(String username, String password, String email, String firstname, String lastname, String phoneNumber) throws Exception {
        LOGGER.info("----addUser");

        Account testAccount = new Account(username, password);
        assertTrue(serverController.addUser(testAccount, email, firstname, lastname, phoneNumber, ""));
        assertFalse(serverController.addUser(testAccount, email, firstname, lastname, phoneNumber, ""));
        return testAccount;
    }

    public boolean authenticate(String username, String password) throws Exception {
        LOGGER.info("----authenticate");

        return serverController.authenticate(new Account(username, password));
    }

    public void createKeychain() throws Exception {
        LOGGER.info("----createKeychain");

        directoryController.createKeychain("testServerController1");
        DirectoryEntry mykeychain = directoryController.getKeychains().get(0);
        Boolean condition = serverController.createKeychain(account, mykeychain);
        assertTrue(condition);
    }

    public void setKeychain() throws Exception {
        LOGGER.info("----setKeychain");
        Keychain keychain = directoryController.getKeychain("testServerController1").get();
        directoryController.createPassword(keychain, "password1",  "testuser",Crypto.secretKeyFromBytes("secret1".getBytes()));
        serverController.setKeychain(account, directoryController.getKeychains().get(0));
    }

    public void getKeychains() throws Exception {
        LOGGER.info("----getKeychains");
        ArrayList<Long> serverKeychains = (ArrayList) serverController.getKeychains(account);
        ArrayList<Long> directoryKeychains = new ArrayList<Long>();
        for (int i = 0; i < directoryController.getKeychains().size(); i++) {
            directoryKeychains.add(directoryController.getKeychains().get(i).getServerid());
        }
        assertEquals(directoryKeychains,serverKeychains);

    }

    public void deleteKeychain() throws Exception {
        LOGGER.info("----deleteKeychain");
        assertTrue(serverController.deleteKeychain(account, directoryController.getKeychains().get(0).getServerid()));
    }

    private class TestDataContainer {
        public boolean success = false;
    }
}