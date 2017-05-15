package postit.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.cli.*;
import postit.server.database.MySQL;
import postit.server.netty.RequestInitializer;
import postit.shared.Crypto;

import javax.net.ssl.SSLContext;
import java.sql.SQLException;

/**
 * Created by dog on 3/8/2017.
 */
public class ServerApp {
    public static void main(String[] args){
        int rePort = 2048;

        Option port = Option.builder().longOpt("port").hasArg(true).build();

        Option db_name = Option.builder().longOpt("db-name").hasArg(true).build();
        Option db_username  = Option.builder().longOpt("db-username").hasArg(true).build();
        Option db_password = Option.builder().longOpt("db-password").hasArg(true).build();
        Option db_address  = Option.builder().longOpt("db-address").hasArg(true).build();

        Option db_should_initialise  = Option.builder().longOpt("db-initialise").hasArg(false).build();


        Options options = new Options();
        options.addOption("p", true, "port");
        options.addOption(port);
        options.addOption(db_name);
        options.addOption(db_username);
        options.addOption(db_password);
        options.addOption(db_address);
        options.addOption(db_should_initialise);

        DefaultParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("ERROR PARSING OPTIONS: " + e.getMessage());
            return;
        }

        MySQL database;

        if (cmd.hasOption("db-name") && cmd.hasOption("db-username") && cmd.hasOption("db-password") && cmd.hasOption("db-address")) {
            try {
                database = MySQL.remoteDatabase(cmd.getOptionValue("db-address"), cmd.getOptionValue("db-name"), cmd.getOptionValue("db-username"), cmd.getOptionValue("db-password"));
            } catch (SQLException e) {
                System.err.println(("Error connecting to SPECIFIED database...: " + e.getMessage()));
                return;
            }
        } else {
            if (cmd.hasOption("db-name") || cmd.hasOption("db-username") || cmd.hasOption("db-password") || cmd.hasOption("db-address")) {
                System.err.println("Most provide all data base parameters.");
                return;
            }

            try {
                database = MySQL.defaultDatabase();
            } catch (SQLException e) {
                System.err.println(("Error connecting to TEST database...: " + e.getMessage()));
                return;
            }
        }

        if (cmd.hasOption("db-initialise")) {
            if (database.initDatabase()) {
                System.out.println("Successfully initialised DB!");
            } else {
                System.err.println("Failed to initialise DB!");
                return;
            }
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