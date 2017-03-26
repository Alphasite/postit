package postit.server.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
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
	private String user;
	String password;
	String url;

	ComboPooledDataSource cpds;

	public MySQL(String url, String database, String user, String pwd) throws SQLException {
		this.password = pwd;
		this.user = user;
		this.databaseName = database;
		this.url = url;

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
			URL resource = ClassLoader.getSystemClassLoader().getResource("./database/init_schema.sql");
			byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
			String sql = new String(bytes);
			return statement.execute(sql);
		} catch (SQLException | IOException | URISyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}
}
