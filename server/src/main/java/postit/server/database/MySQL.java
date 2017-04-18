package postit.server.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;

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

	ComboPooledDataSource cpds;

	public MySQL(String url, String database, String user, String pwd) throws SQLException {
		this.password = pwd;
		//this.user = user;
		this.databaseName = database;
		//this.url = url;

		try {
			this.cpds = new ComboPooledDataSource();
			this.cpds.setDriverClass("com.mysql.cj.jdbc.Driver");
			this.cpds.setJdbcUrl("jdbc:mysql://" + url + "/" + databaseName + "?useSSL=false");
			this.cpds.setUser(user);
			this.cpds.setPassword(password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MySQL localDatabase(String database, String user, String pwd) throws SQLException {
		return new MySQL("localhost:3306", database, user, pwd);
	}

	public static MySQL remoteDatabase(String database, String user, String pwd) throws SQLException {
		return new MySQL("nishadmathur.com:3306", database, user, pwd);
	}

	public static MySQL defaultDatabase() throws SQLException {
		return MySQL.remoteDatabase("postit", "postit", "xDljSX8Ojk");
	}

	@Override
	public Connection connect() throws SQLException {
		return this.cpds.getConnection();
	}

	@Override
	public boolean initDatabase() {
		try (Connection connection = connect(); Statement statement = connection.createStatement()){
			final String sql = DatabaseUtils.getSetupSQL();
            PreparedStatement ps = connection.prepareStatement("?");
            ps.setString(1,sql);
			Boolean res = ps.execute();
            ps.close();
            return res;

		} catch (SQLException | IOException | URISyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}
}
