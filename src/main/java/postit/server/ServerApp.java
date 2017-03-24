package postit.server;

import postit.server.database.MySQL;
import postit.shared.communication.ServerReceiver;
import postit.shared.communication.ServerSender;

import java.sql.SQLException;

/**
 * Created by dog on 3/8/2017.
 */
public class ServerApp {
    public static void main(String[] args){
        int rePort = 2048;
        int outPort = 4880;

        MySQL database;

        try {
            database = MySQL.defaultDatabase();
        } catch (SQLException e) {
            System.err.println(("Error connecting to database...: " + e.getMessage()));
            return;
        }

        ServerSender processor = new ServerSender(4880, database);
        ServerReceiver receiver = new ServerReceiver(2048, processor);
        Thread t1 = new Thread(processor);
        Thread t2 = new Thread(receiver);

        t1.start();
        t2.start();

    }
}