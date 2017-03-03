package postit.client.handler;

import javax.json.JsonObject;

/**
 * Class handling requests from frontend and directs to the proper backend controller.
 * Changes needed in future:
 * - an interface for Handler
 * - event response JSONObject or CODE instead of boolean
 * - privatize the handling functions
 * @author Ning
 *
 */
public class AccountHandler {

	public boolean authenticate(String username, String pwd){
		/**
		 * Account act = db.getAccount(username);
		 * compute pwd_key from pwd
		 * if (pwd_key == act.pwd_key) return true;
		 * else return false
		 */
		return false;
	}
	
	public boolean addAccount(String username, String pwd, String email, String firstname, String lastname){
		/**
		 * Account act = new Account(username, pwd, email, firstname, lastname);
		 * encryption on pwd
		 * if (db.getAccount(username) == null) db.addAccount(act); return true;
		 * else return false;
		 */
		return false;
	}
	
	public boolean updateAccount(String username, String pwd, String email, String firstname, String lastname){
		/**
		 * Account act = new Account(username, pwd, email, firstname, lastname);
		 * encryption on pwd
		 * db.updateAccount(act); 
		 * return false if db operation failed
		 */
		return false;
	}
	
	public boolean removeAccount(String username, String pwd){
		/**
		 * Account act = db.getAccount(username);
		 * compute pwd_key from pwd
		 * if (pwd_key == act.pwd_key) db.removeAccount(username); return true;
		 * else return false
		 */
		return false;
	}
	
	public JsonObject getAccounts(){
		// Change return type to List<Account> or List<JsonObject> with just info to be displayed
		// For developer use only, to see list of accounts
		// return db.getAccounts();
		return null;
	}
}
