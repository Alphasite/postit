package postit.server.controller;

import org.junit.Before;
import org.junit.Test;
import postit.server.database.Database;
import postit.server.database.TestH2;

import java.security.SecureRandom;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * 
 * @author Ning
 *
 */
public class ServerAccountHandlerTest {
    Database database;
    DatabaseController db;
    AccountHandler ah;

    @Before
    public void setUp() throws Exception {
        database = new TestH2();
        db = new DatabaseController(database);
        ah = new AccountHandler(db, new SecureRandom());
        database.initDatabase();
    }

	public static void testAuthentication(AccountHandler ah, String username, String pwd, boolean expected){
		boolean res = ah.authenticate(username, pwd);
		System.out.printf("Authenticate %s with %s %s%n", username, pwd, res ? "successful" : "failed");
		assertThat(res, is(expected));
	}
	
	public static void testAddAccount(AccountHandler ah, String username, String pwd, String email, 
			String fName, String lName, String phone, boolean expected){

		boolean res = ah.addAccount(username, pwd, email, fName, lName, "keypair", phone);
		System.out.printf("Adding account %s with %s %s%n", username, pwd, res ? "successful" : "failed");
		assertThat(res, is(expected));
	}
	
	public static void testUpdateAccount(AccountHandler ah, String username, String pwd, String email, 
			String fName, String lName, String phone, boolean expected){
		boolean res = ah.updateAccount(username, pwd, email, fName, lName, phone);
		System.out.printf("Updating account %s with %s %s%n", username, pwd, res ? "successful" : "failed");
		assertThat(res, is(expected));
	}
	
	public static void testRemoveAccount(AccountHandler ah, String username, String pwd, boolean expected){
		boolean res = ah.removeAccount(username, pwd);
		System.out.printf("Removing account %s with %s %s%n", username, pwd, res ? "successful" : "failed");
		assertThat(res, is(expected));
	}
	
	@Test
	public void runTest(){
		// authentication
		testAuthentication(ah, "ning", "5431", true);
		testAuthentication(ah, "ning", "wrong!", false);
		testAuthentication(ah, "mc", "cs5431", false);
		
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c", "8000000000", false);
		testAddAccount(ah, "mc", "lalala", "mc@cornell.edu", "m", "c", "8000000000", true);
		testUpdateAccount(ah, "mc", "cs5431", "mc@cornell.edu", "m", "c", "8000000000", true);
		testAuthentication(ah, "mc", "cs5431", true);
		testRemoveAccount(ah, "mc", "lalalal", false);
		testRemoveAccount(ah, "mc", "cs5431", true);
	}
	
	public static void main(String[] args) throws Exception{
		ServerAccountHandlerTest test = new ServerAccountHandlerTest();
		test.setUp();
		testAuthentication(test.ah, "ning", "5431", false);
	}
}
