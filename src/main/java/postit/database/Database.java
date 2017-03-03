package postit.database;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Managing local and remote database connection and setup.
 * @author Ning
 *
 */
public class Database {
	
	private static final Logger LOGGER = Logger.getLogger(Database.class.getName());

	/**
	 * 
	 * @return a connection allowing to manipulate existing schema
	 */
	public static Connection connectToDefault() {
		return connectToLocal("pwddb", "root", "5431");
	}

	public static Connection connectToLocal(String database, String username, String password){
		return LocalMySQL.getConnection(database, username, password);
	}

}
