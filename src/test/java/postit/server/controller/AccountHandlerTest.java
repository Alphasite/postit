package postit.server.controller;

import postit.server.controller.AccountHandler;
import postit.server.controller.DatabaseController;

/**
 * 
 * @author Ning
 *
 */
public class AccountHandlerTest {

	public static void testAuthentication(DatabaseController db, AccountHandler ah, String username, String pwd){
		boolean res = ah.authenticate(db, username, pwd);
		System.out.printf("Authenticate %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testAddAccount(DatabaseController db, AccountHandler ah, String username, String pwd, String email, String fName, String lName){
		boolean res = ah.addAccount(db, username, pwd, email, fName, lName);
		System.out.printf("Adding account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testUpdateAccount(DatabaseController db, AccountHandler ah, String username, String pwd, String email, String fName, String lName){
		boolean res = ah.updateAccount(db, username, pwd, email, fName, lName);
		System.out.printf("Updating account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testRemoveAccount(DatabaseController db, AccountHandler ah, String username, String pwd){
		boolean res = ah.removeAccount(db, username, pwd);
		System.out.printf("Removing account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void main(String[] args){
		DatabaseController db = new DatabaseController();
		AccountHandler ah = new AccountHandler();
		
		// authentication
		testAuthentication(db, ah, "ning", "5431");
		testAuthentication(db, ah, "ning", "wrong!");
		testAuthentication(db, ah, "mc", "cs5431");
		
		testUpdateAccount(db, ah, "mc", "cs5431", "mc@cornell.edu", "m", "c");
		testAddAccount(db, ah, "mc", "lalala", "mc@cornell.edu", "m", "c");
		testUpdateAccount(db, ah, "mc", "cs5431", "mc@cornell.edu", "m", "c");
		testAuthentication(db, ah, "mc", "cs5431");
		testRemoveAccount(db, ah, "mc", "lalalal");
		testRemoveAccount(db, ah, "mc", "cs5431");
	}
}
