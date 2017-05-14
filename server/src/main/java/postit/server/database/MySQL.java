package postit.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Functions related to local MySQL database.
 * @author Ning
 *
 */
@SuppressWarnings("ALL")
public class MySQL implements Database {
	String databaseName;
	//private String user;
	String password;
	//String url;

	HikariDataSource hds;

	public MySQL(String url, String database, String user, String pwd) throws SQLException {
		this.password = pwd;
		//this.user = user;
		this.databaseName = database;
		//this.url = url;

		try {
			HikariConfig jdbcConfig;
			jdbcConfig = new HikariConfig();
			jdbcConfig.setJdbcUrl("jdbc:mysql://" + url + "/" + databaseName + "?useSSL=false");
			jdbcConfig.setUsername(user);
			jdbcConfig.setPassword(password);
			hds = new HikariDataSource(jdbcConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MySQL localDatabase(String database, String user, String pwd) throws SQLException {
		return new MySQL("localhost:3306", database, user, pwd);
	}

	public static MySQL remoteDatabase(String address, String database, String user, String pwd) throws SQLException {
		return new MySQL(address, database, user, pwd);
	}

	public static MySQL defaultDatabase() throws SQLException {
		return new MySQL("nishadmathur.com:3306","postit", "postit", "xDljSX8Ojk");
	}

	@Override
	public Connection connect() throws SQLException {
		return this.hds.getConnection();
	}

	@Override
	public boolean initDatabase() {
		try (Connection connection = connect(); Statement statement = connection.createStatement()){
			final String sql = DatabaseUtils.getSetupSQL();
            PreparedStatement ps = connection.prepareStatement("?");
            ps.setString(1, sql);
			Boolean res = ps.execute();
            ps.close();
            return res;
		} catch (SQLException | IOException | URISyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}
}
