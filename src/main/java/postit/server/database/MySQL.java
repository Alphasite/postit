package postit.server.database;

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
public class MySQL implements Database {
	String databaseName;
	String user;
	String password;
	String url;

	public MySQL(String url, String database, String user, String pwd) throws SQLException {
		this.password = pwd;
		this.user = user;
		this.databaseName = database;
		this.url = url;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
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
		return DriverManager.getConnection("jdbc:mysql://" + url + "/" + databaseName + "?useSSL=false", user, password);
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
