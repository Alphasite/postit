package postit.server.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import postit.server.controller.AccountHandler;
import postit.server.controller.DatabaseController;
import postit.server.controller.KeychainHandler;
import postit.server.controller.LogController;
import postit.server.controller.RequestHandler;
import postit.server.database.Database;
import postit.shared.Crypto;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.SecureRandom;

/**
 *
 * Created by nishadmathur on 25/3/17.
 */
public class RequestInitializer extends ChannelInitializer<SocketChannel> {
    private final SSLContext context;
    private final AccountHandler accountHandler;
    private final KeychainHandler keychainHandler;
    private final LogController logController;

    public RequestInitializer(SSLContext context, Database database) {
        DatabaseController db = new DatabaseController(database);

        this.accountHandler = new AccountHandler(db, Crypto.getRandom());
        this.keychainHandler = new KeychainHandler(db);
        this.logController = new LogController(db);
        this.context = context;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast();

        SSLEngine engine = context.createSSLEngine();
        engine.setEnabledCipherSuites(new String[]{"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"});
        engine.setUseClientMode(false);

        SslHandler sslHandler = new SslHandler(engine);
        sslHandler.setHandshakeTimeoutMillis(5000);

        // Add SSL handler first to encrypt and decrypt everything.
        // Split the message into lines, then convert it into a string.
        // Then let the line handler handle it.
        pipeline.addLast(
                sslHandler,
                new LineBasedFrameDecoder(1024 * 1024, true, true),
                new StringDecoder(),
                new StringEncoder(),
                new RequestHandler(accountHandler, keychainHandler, logController) // And our handler.
        );
    }
}
