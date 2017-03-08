package postit.server.controller;

import javax.json.JsonObject;

import postit.shared.model.Account;

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
	
	private DatabaseController db;
	
	public AccountHandler(DatabaseController db){
		this.db = db;
	}

	/**
	 * Given username and master password pwd, checks if the user authenticates.
	 * Returns false if username and pwd do not autheticate.
	 * @param db
	 * @param username
	 * @param pwd
	 * @return
	 */
	public boolean authenticate(String username, String pwd){
		Account account = db.getAccount(username);
		// TODO pwd = generateKey(pwd);
		if (account != null && pwd.equals(account.getPassword())) 
			return true;
		return false;
	}
	
	/**
	 * Creates a new user with given information.
	 * @param db
	 * @param username
	 * @param pwd
	 * @param email
	 * @param firstname
	 * @param lastname
	 * @return
	 */
	public boolean addAccount(String username, String pwd, String email, String firstname, String lastname){
		return addAccount(new Account(username, pwd, email, firstname, lastname)); //TODO encryption on pwd
	}
	
	public boolean addAccount(Account account){
		if (db.getAccount(account.getUsername()) == null){
			if (db.addAccount(account)){
				return db.addDirectory(account.getUsername(), ".").getString("status").equals("success");
			}
		}
		return false;
	}
	
	public boolean updateAccount(String username, String pwd, String email, String firstname, String lastname){
		Account account = new Account(username, pwd, email, firstname, lastname); //TODO encryption on pwd
		return updateAccount(account);
	}
	
	public boolean updateAccount(Account account){
		return db.updateAccount(account);
	}
	
	public boolean removeAccount(String username, String pwd){
		Account account = db.getAccount(username);
		// TODO pwd = generateKey(pwd);
		if (pwd.equals(account.getPassword())){
			return db.removeAccount(username) && db.removeDirectory(username);
		}
		return false;
	}
	
	public Account getAccount(String username){
		return db.getAccount(username);
	}
	
	public JsonObject getAccounts(){
		// Change return type to List<Account> or List<JsonObject> with just info to be displayed
		// For developer use only, to see list of accounts
		// return db.getAccounts();
		return null;
	}
}
