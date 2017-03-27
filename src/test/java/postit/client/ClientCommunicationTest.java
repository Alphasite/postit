package postit.client;

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
import postit.client.controller.DirectoryController;
import postit.client.controller.RequestMessenger;
import postit.client.controller.ServerController;
import postit.client.keychain.Account;
import postit.client.keychain.Directory;
import postit.server.database.Database;
import postit.server.database.TestH2;
import postit.server.netty.RequestInitializer;
import postit.shared.Crypto;
import postit.shared.communication.Client;

import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.SSLContext;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;


public class ClientCommunicationTest {
    Client client;

    private MockKeyService keyService;
    private MockBackingStore backingStore;
    private Directory directory;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Thread thread;

    @Before
    public void setUp() throws Exception {
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

        Runnable server = () -> {
            try {
                System.out.println("Waiting for close server...");
                channel.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        };

        thread = new Thread(server);
        thread.start();

        client = new Client(2048, "localhost");
    }

    @After
    public void tearDown() throws Exception {
        thread.stop();
        bossGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).sync();
        workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).sync();
    }

    public static JsonObject testRequest(String request, Client client) {
        System.out.println("Sending to server: " + request);
        Optional<JsonObject> response = client.send(request);
        assertThat(response.isPresent(), is(true));
        System.out.println("Response: " + response);
        return response.get();
    }


    @Test
    public void testCommunication() {
        Account account = new Account("ning", "5431");
        String req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("ning", "5430");
        req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("mc", "5431");
        req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("ning", "5431");

        req = RequestMessenger.createAddKeychainsMessage(account, "fb", "hihi");
        int fbId = testRequest(req, client).getJsonObject("keychain").getInt("directoryEntryId");

        req = RequestMessenger.createUpdateKeychainMessage(account, fbId, "fb", "lala");
        testRequest(req, client);

        req = RequestMessenger.createAddKeychainsMessage(account, "net", "hihi");
        int netid = testRequest(req, client).getJsonObject("keychain").getInt("directoryEntryId");

        req = RequestMessenger.createUpdateKeychainMessage(account, netid, "net", "lala");
        testRequest(req, client);

        req = RequestMessenger.createGetKeychainsMessage(account);
        testRequest(req, client);

        req = RequestMessenger.createRemoveKeychainMessage(account, netid);
        testRequest(req, client);

        req = RequestMessenger.createRemoveKeychainMessage(account, fbId);
        testRequest(req, client);

        req = RequestMessenger.createGetKeychainsMessage(account);
        testRequest(req, client);
    }
}
