package postit.server.database;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Random;

/**
 * Created by nishadmathur on 22/3/17.
 */
public class TestH2 implements Database, Closeable {
    DB db;
    DBConfigurationBuilder configBuilder;

    public TestH2() throws ManagedProcessException {
        configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(0); // OR, default: setPort(0); => autom. detect free port
        db = DB.newEmbeddedDB(configBuilder.build());
        db.start();
    }

    @Override
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(configBuilder.getURL("postit") + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
    }

    @Override
    public boolean initDatabase() {
        try {
            db.createDB("postit");
            db.source("./database/init_schema.sql", null, null, "postit");
            return true;
        } catch (ManagedProcessException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            db.stop();
        } catch (ManagedProcessException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
}
