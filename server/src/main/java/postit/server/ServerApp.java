package postit.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import postit.server.database.MySQL;
import postit.server.netty.RequestInitializer;
import postit.shared.Crypto;

import javax.net.ssl.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by dog on 3/8/2017.
 */
public class ServerApp {
    public static void main(String[] args){
        int rePort = 2048;

        MySQL database;

        try {
            database = MySQL.defaultDatabase();
        } catch (SQLException e) {
            System.err.println(("Error connecting to database...: " + e.getMessage()));
            return;
        }

        Crypto.init();

        SSLContext ctx = Crypto.getSSLContext();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new RequestInitializer(ctx, database));

            b.bind(rePort).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}