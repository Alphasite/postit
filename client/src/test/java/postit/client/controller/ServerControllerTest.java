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
import postit.client.keychain.Account;
import postit.client.keychain.Directory;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.Keychain;
import postit.server.database.Database;
import postit.server.database.TestH2;
import postit.server.netty.RequestInitializer;
import postit.shared.Crypto;

import javax.net.ssl.SSLContext;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.nullValue;
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

        serverController.addUser(account, "test@test.com", "te", "st", "8000000000");
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
        addUser("testServerController", "password", "test@servercontroller.com", "test", "server", "8000000000");
        assertTrue(authenticate("testServerController", "password"));
        assertFalse(authenticate("NOTtestServerController", "password"));
        assertFalse(authenticate("testServerController", "NOTpassword"));

        ServerControllerTest serverControllerTest = this;

        createKeychain();
        setKeychain();
        synchronized (serverControllerTest) {
            serverController.sync(() -> {
                synchronized (serverControllerTest) {
                    serverControllerTest.notifyAll();
                }
            });

            this.wait(10000);
        }
        getKeychains();
        deleteKeychain();
    }

    public void addUser(String username, String password, String email, String firstname, String lastname, String phoneNumber) throws Exception {
        LOGGER.info("----addUser");

        Account testAccount = new Account(username, password);
        assertTrue(serverController.addUser(testAccount, email, firstname, lastname, phoneNumber));
        assertFalse(serverController.addUser(testAccount, email, firstname, lastname, phoneNumber));
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
}