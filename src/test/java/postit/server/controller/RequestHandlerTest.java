package postit.server.controller;

import org.junit.Before;
import postit.client.controller.RequestMessenger;

import org.junit.Test;
import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import postit.client.keychain.Account;
import postit.server.database.TestH2;
import postit.shared.Crypto;

public class RequestHandlerTest {
	private RequestHandler rh;
	private TestH2 database;

	private static void testAuthentication(RequestHandler rh, String username, String pwd, boolean expected){
		System.out.printf("Authenticate %s with %s\n", username, pwd);
		String req = RequestMessenger.createAuthenticateMessage(new Account(username, pwd));
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}

	private static void testAddKeychain(RequestHandler rh, Account account, String name, String pwd, boolean expected){
		System.out.printf("Adding keychain to %s: (%s, %s)\n", account, name, pwd);
		String req = RequestMessenger.createAddKeychainsMessage(account, name, "haha", pwd, "nothing");
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}

	private static void testUpdateKeychain(RequestHandler rh, Account account, String name,
										   String encryptKey, String password, String metadata, boolean expected){
		System.out.printf("Updating keychain to %s (%s,%s,%s,%s)\n", account, name, encryptKey, password, metadata);
		String req = RequestMessenger.createUpdateKeychainMessage(account, name, encryptKey, password, metadata);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}

	private static void testGetKeychains(RequestHandler rh, Account account, int expectedNumKeychains){
		System.out.println("Getting keychains of " + account);
		String req = RequestMessenger.createGetKeychainsMessage(account);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success"));
		JSONArray ja = js.getJSONArray("keychains");
		assertEquals(expectedNumKeychains, ja.length());
	}

	private static void testRemoveKeychain(RequestHandler rh, Account account, String name, boolean expected){
		System.out.printf("Removing keychain %s from %s\n", name, account);
		String req = RequestMessenger.createRemoveKeychainMessage(account, name);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}

	@Before
	public void setUp() throws Exception {
		database = new TestH2();
		database.initDatabase();

		DatabaseController controller = new DatabaseController(database);
		AccountHandler accountHandler = new AccountHandler(controller, Crypto.getRandom());
		KeychainHandler keychainHandler = new KeychainHandler(controller);

		rh = new RequestHandler(accountHandler, keychainHandler);
	}

	@Test
	public void runTest() {
		testAuthentication(rh, "ning", "5431", true);
		testAuthentication(rh, "ning", "5430", false);
		testAuthentication(rh, "mc", "5431", false);

		Account account = new Account("ning, 5430");

		testGetKeychains(rh, account, 2);
		testAddKeychain(rh, account, "fb", "1234", true);
		testUpdateKeychain(rh, account, "fb", "lala", "4321", "nothing", true);
		testUpdateKeychain(rh, account, "net", "lala", "4321", "nothing", false);
 		testGetKeychains(rh, account, 3);
		
		testRemoveKeychain(rh, account, "net", false);
		testRemoveKeychain(rh, account, "fb", true);
		testGetKeychains(rh, account, 2);
	}

}
