package postit.server.controller;

import postit.server.model.ServerAccount;

import javax.json.JsonObject;
import java.security.SecureRandom;

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
	public boolean addAccount(String username, String pwd, String email, String firstname, String lastname, String keypair, String publickey, String phoneNumber) {
		return addAccount(new ServerAccount(username, pwd, email, firstname, lastname, phoneNumber, keypair, publickey));
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
	
	public boolean updateAccount(String username, String pwd, String email, String firstname, String lastname, 
			String phoneNumber){
		ServerAccount serverAccount = new ServerAccount(username, pwd, email, firstname, lastname, phoneNumber);
		if (pwd != null){
			ServerAccount a = db.getAccount(username);
			if (a != null)
				serverAccount.setPassword(Util.hashPassword(pwd, db.getSalt(username)));
			else
				return false;
		}
		return updateAccount(serverAccount);
	}

	public boolean updateAccountEmail(String username, String newEmail) {
		ServerAccount oldAccount = db.getAccount(username);
		if (oldAccount != null) {
			oldAccount.setEmail(newEmail);
		} else {
			return false;
		}
		return updateAccount(oldAccount);
	}

	public boolean updateAccountPassword(String username, String newpwd) {
		ServerAccount oldAccount = db.getAccount(username);
		if (oldAccount != null) {
			oldAccount.setPassword(Util.hashPassword(newpwd, db.getSalt(username)));
		} else {
			return false;
		}
		return updateAccount(oldAccount);
	}
	
	public boolean updateAccount(ServerAccount serverAccount){
		ServerAccount account = new ServerAccount();
		ServerAccount server = getAccount(serverAccount.getUsername());
		account.setUsername(serverAccount.getUsername());
		account.setPassword(serverAccount.getPassword() == null ? server.getPassword() : serverAccount.getPassword());
		account.setEmail(serverAccount.getEmail() == null ? server.getEmail() : serverAccount.getEmail());
		account.setFirstname(serverAccount.getFirstname() == null ? server.getFirstname() : serverAccount.getFirstname());
		account.setLastname(serverAccount.getLastname() == null ? server.getLastname() : serverAccount.getLastname());
		account.setPhoneNumber(serverAccount.getPhoneNumber() == null ? server.getPhoneNumber() : serverAccount.getPhoneNumber());
		return db.updateAccount(account);
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

	public String getKeyPair(String username) {return db.getKeyPair(username); }
	
	public JsonObject getAccounts(){
		// Change return type to List<ServerAccount> or List<JsonObject> with just info to be displayed
		// For developer use only, to see list of accounts
		// return db.getAccounts();
		return null;
	}
}
