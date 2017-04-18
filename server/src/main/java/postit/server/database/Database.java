package postit.server.database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by nishadmathur on 22/3/17.
 */
public interface Database {
    Connection connect() throws SQLException;

    boolean initDatabase();

    String getSetupSQL() throws IOException, URISyntaxException;
}
