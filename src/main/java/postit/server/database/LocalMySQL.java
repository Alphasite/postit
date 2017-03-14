package postit.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Functions related to local MySQL database.
 * @author Ning
 *
 */
public class LocalMySQL {

	private static Connection conn;
	public static Connection getConnection(String database, String user, String pwd) throws SQLException {

		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + database + "?useSSL=false", user, pwd);

		return conn;
	}

}
