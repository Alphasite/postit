package handler;

import javax.json.JsonObject;

import keychain.Directory;

import java.util.Map;

/**
 * Class handling requests from frontend and directs to the proper backend controller.
 * Changes needed in future:
 * - change metadata's type
 * - an interface for Handler
 * - event response JSONObject or CODE instead of boolean
 * - privatize the handling functions
 * @author Ning
 *
 */
public class KeychainHandler {

	public boolean createKeychain(String name){
		/**
		 * DirectoryEntry de = new DirectoryEntry(name, path?, generated_encryption_key);
		 * de.keychain = new Keychain(name, pwd);
		 * perform necessary encryption
		 * if (db.getKeychain(name) == null) db.addKeychain(de); //series of db updates to Directory, DirectoryEntry, etc
		 * else return false;
		 */
		return false;
	}
	
	public boolean updateKeychain(String username, String pwd, String email, String firstname, String lastname){
		/**
		 * DirectoryEntry de = new DirectoryEntry(name, path?, generated_encryption_key);
		 * de.keychain = new Keychain(name, pwd);
		 * perform necessary encryption
		 * db.updateKeychain(de); //series of db updates to Directory, DirectoryEntry, etc
		 * return false if any operation fails
		 */
		return false;
	}
	
	public boolean removeKeychain(String username, String name){
		/**
		 * Assumed user properly logged in and established authenticity
		 * Directory dir = db.getDirectory(username);
		 * if (dir.contains(name) db.removeKeychain(name); dir.removeKeychain(name); etc
		 * return false if any operation fails
		 */
		return false;
	}
	
	public Directory getKeychains(String username){
		/**
		 * Assuming authentication
		 * Directory dir = db.getDirectory(username); return dir;
		 * return null if operation failed.
		 */
		// Or returns a list of information to be displayed instead of the actual object
		return null;
	}
}
