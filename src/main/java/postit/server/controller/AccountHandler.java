package postit.server.controller;

import javax.json.JsonObject;

import postit.server.model.*;

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

	/**
	 * Given username and master password pwd, checks if the user authenticates.
	 * Returns false if username and pwd do not autheticate.
	 * @param db
	 * @param username
	 * @param pwd
	 * @return
	 */
	public boolean authenticate(DatabaseController db, String username, String pwd){
		Account account = db.getAccount(username);
		// TODO pwd = generateKey(pwd);
		if (account != null && pwd.equals(account.getPassword())) 
			return true;
		return false;
	}
	
	public boolean addAccount(DatabaseController db, String username, String pwd, String email, String firstname, String lastname){
		Account account = new Account(username, pwd, email, firstname, lastname); //TODO encryption on pwd
		if (db.getAccount(username) == null){
			db.addAccount(account); 
			return true;
		}
		return false;
	}
	
	public boolean updateAccount(DatabaseController db, String username, String pwd, String email, String firstname, String lastname){
		Account account = new Account(username, pwd, email, firstname, lastname); //TODO encryption on pwd
		return db.updateAccount(account);
	}
	
	public boolean removeAccount(DatabaseController db, String username, String pwd){
		Account account = db.getAccount(username);
		// TODO pwd = generateKey(pwd);
		if (pwd.equals(account.getPassword())) 
			return db.removeAccount(username);
		return false;
	}
	
	public JsonObject getAccounts(){
		// Change return type to List<Account> or List<JsonObject> with just info to be displayed
		// For developer use only, to see list of accounts
		// return db.getAccounts();
		return null;
	}
}
