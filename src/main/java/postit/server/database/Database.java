package postit.server.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by nishadmathur on 22/3/17.
 */
public interface Database {
    Connection connect() throws SQLException;

    boolean initDatabase();
}
