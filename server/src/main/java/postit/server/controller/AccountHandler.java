package postit.server.controller;

import java.security.SecureRandom;

import javax.json.JsonObject;

import postit.server.model.ServerAccount;

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
	private SecureRandom rand;
	
	public AccountHandler(DatabaseController db, SecureRandom rand){
		this.db = db;
		this.rand = rand;
	}

	/**
	 * Given username and master password pwd, checks if the user authenticates.
	 * Returns false if username and pwd do not autheticate.
	 * @param username
	 * @param pwd
	 * @return
	 */
	public boolean authenticate(String username, String pwd){
		ServerAccount serverAccount = db.getAccount(username);
		if (serverAccount != null) {
			return Util.comparePasswords(pwd, db.getSalt(username), db.getPassword(username));
		}
		return false;
	}
	
	/**
	 * Creates a new user with given information.
	 * @param username
	 * @param pwd
	 * @param email
	 * @param firstname
	 * @param lastname
	 * @return
	 */
	public boolean addAccount(String username, String pwd, String email, String firstname, String lastname){
		return addAccount(new ServerAccount(username, pwd, email, firstname, lastname));
	}
	
	public boolean addAccount(ServerAccount serverAccount){
		if (db.getAccount(serverAccount.getUsername()) == null){
			ServerAccount a = new ServerAccount(serverAccount);
			a.setSalt(Util.generateSalt(rand, 4));
			a.setPassword(Util.hashPassword(a.getPassword(), a.getSalt()));
			return db.addAccount(a);
		}
		return false;
	}
	
	public boolean updateAccount(String username, String pwd, String email, String firstname, String lastname){
		ServerAccount serverAccount = new ServerAccount(username, pwd, email, firstname, lastname);
		if (pwd != null){
			ServerAccount a = db.getAccount(username);
			if (a != null)
				serverAccount.setPassword(Util.hashPassword(pwd, db.getSalt(username)));
			else
				return false;
		}
		return updateAccount(serverAccount);
	}
	
	public boolean updateAccount(ServerAccount serverAccount){
		return db.updateAccount(serverAccount);
	}
	
	public boolean removeAccount(String username, String pwd){
		ServerAccount serverAccount = db.getAccount(username);
		if (serverAccount != null && Util.comparePasswords(pwd, db.getSalt(username), db.getPassword(username))){
			return db.removeAccount(username); // TODO remove orphaned keychains
		}
		return false;
	}
	
	public ServerAccount getAccount(String username){
		return db.getAccount(username);
	}
	
	public JsonObject getAccounts(){
		// Change return type to List<ServerAccount> or List<JsonObject> with just info to be displayed
		// For developer use only, to see list of accounts
		// return db.getAccounts();
		return null;
	}
}
