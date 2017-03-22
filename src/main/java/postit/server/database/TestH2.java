package postit.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by nishadmathur on 22/3/17.
 */
public class TestH2 {
    private static Connection conn;

    public static Connection getConnection(String database) throws SQLException {

        if (conn == null) {
            try {
                Class.forName("org.h2.Driver").newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }

            conn = DriverManager.getConnection("jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1");
        }

        return conn;
    }
}
