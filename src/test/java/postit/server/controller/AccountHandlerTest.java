package postit.server.controller;

import org.junit.Test;
import static org.junit.Assert.*;

import postit.server.controller.AccountHandler;
import postit.server.controller.DatabaseController;

/**
 * 
 * @author Ning
 *
 */
public class AccountHandlerTest {

	public static void testAuthentication(AccountHandler ah, String username, String pwd, boolean expected){
		boolean res = ah.authenticate(username, pwd);
		System.out.printf("Authenticate %s with %s %s\n", username, pwd, res ? "successful" : "failed");
		assertTrue(res == expected);
	}
	
	public static void testAddAccount(AccountHandler ah, String username, String pwd, String email, 
			String fName, String lName, boolean expected){
		boolean res = ah.addAccount(username, pwd, email, fName, lName);
		System.out.printf("Adding account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
		assertTrue(res == expected);
	}
	
	public static void testUpdateAccount(AccountHandler ah, String username, String pwd, String email, 
			String fName, String lName, boolean expected){
		boolean res = ah.updateAccount(username, pwd, email, fName, lName);
		System.out.printf("Updating account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
		assertTrue(res == expected);
	}
	
	public static void testRemoveAccount(AccountHandler ah, String username, String pwd, boolean expected){
		boolean res = ah.removeAccount(username, pwd);
		System.out.printf("Removing account %s with %s %s\n", username, pwd, res ? "successful" : "failed");
		assertTrue(res == expected);
	}
	
	@Test
	public void runTest(){
		DatabaseController db = new DatabaseController();
		AccountHandler ah = new AccountHandler(db);
		
		// authentication
		testAuthentication(ah, "ning", "5431", true);
		testAuthentication(ah, "ning", "wrong!", false);
		testAuthentication(ah, "mc", "cs5431", false);
		
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c", false);
		testAddAccount(ah, "mc", "lalala", "mc@cornell.edu", "m", "c", true);
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c", true);
		testAuthentication(ah, "mc", "cs5431", true);
		testRemoveAccount(ah, "mc", "lalalal", false);
		testRemoveAccount(ah, "mc", "cs5431", true);
	}
}
