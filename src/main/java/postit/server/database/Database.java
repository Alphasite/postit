package postit.server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Managing local and remote database connection and setup.
 *
 * @author Ning
 */
public class Database {

    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

    ConnectionTest type;

    public Database(ConnectionTest test) {
        this.type = test;
    }

    public Connection connect() throws SQLException {
        try {
            if (type.equals(ConnectionTest.local)) {
                return connectToLocal("postit", "root", "root");
            } else if (type.equals(ConnectionTest.test)) {
                return connectToTest("postit");
            } else {
                return connectToDefault();
            }
        } catch (SQLException e) {
            LOGGER.severe("FAILED TO CONNECT TO DB: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @return a connection allowing to manipulate existing schema
     * @throws SQLException
     */
    private static Connection connectToDefault() throws SQLException {
        return connectToRemote("postit", "postit", "xDljSX8Ojk");
    }

    private static Connection connectToLocal(String database, String username, String password) throws SQLException {
        return LocalMySQL.getConnection(database, username, password);
    }

    private static Connection connectToRemote(String database, String username, String password) throws SQLException {
        return RemoteMySQL.getConnection(database, username, password);
    }

    private static Connection connectToTest(String database) throws SQLException {
        return TestH2.getConnection(database);
    }

    enum ConnectionTest {
        local, remote, test
    }
}
