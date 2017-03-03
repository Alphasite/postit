package postit.server.database;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Functions related to local MySQL database.
 * @author Ning
 *
 */
public class LocalMySQL {
	
    private static Connection conn;
    public static Connection getConnection(String database, String user, String pwd) {
    	try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + database, user, pwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
		return conn;
    }
    
}
