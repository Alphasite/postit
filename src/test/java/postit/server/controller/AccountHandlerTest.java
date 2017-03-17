package postit.server.controller;

import postit.server.controller.AccountHandler;
import postit.server.controller.DatabaseController;

/**
 * 
 * @author Ning
 *
 */
public class AccountHandlerTest {

	public static void testAuthentication(AccountHandler ah, String username, String pwd){
		boolean res = ah.authenticate(username, pwd);
		System.out.printf("Authenticate %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testAddAccount(AccountHandler ah, String username, String pwd, String email, String fName, String lName){
		boolean res = ah.addAccount(username, pwd, email, fName, lName);
		System.out.printf("Adding account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testUpdateAccount(AccountHandler ah, String username, String pwd, String email, String fName, String lName){
		boolean res = ah.updateAccount(username, pwd, email, fName, lName);
		System.out.printf("Updating account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void testRemoveAccount(AccountHandler ah, String username, String pwd){
		boolean res = ah.removeAccount(username, pwd);
		System.out.printf("Removing account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
	}
	
	public static void main(String[] args){
		DatabaseController db = new DatabaseController();
		AccountHandler ah = new AccountHandler(db);
		
		// authentication
		testAuthentication(ah, "ning", "5431");
		testAuthentication(ah, "ning", "wrong!");
		testAuthentication(ah, "mc", "cs5431");
		
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c");
		testAddAccount(ah, "mc", "lalala", "mc@cornell.edu", "m", "c");
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c");
		testAuthentication(ah, "mc", "cs5431");
		testRemoveAccount(ah, "mc", "lalalal");
		testRemoveAccount(ah, "mc", "cs5431");
	}
}
