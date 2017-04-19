package postit.server.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import postit.client.controller.RequestMessenger;
import postit.client.keychain.Account;
import postit.server.database.TestH2;
import postit.server.model.ServerKeychain;
import postit.shared.Crypto;
import postit.shared.MessagePackager;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.StringReader;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static postit.shared.MessagePackager.typeToString;

public class RequestHandlerTest {
	private RequestHandler rh;
	private TestH2 database;

	private static void testAuthentication(RequestHandler rh, String username, String pwd, boolean expected){
		System.out.printf("Authenticate %s with %s%n", username, pwd);
		String req = RequestMessenger.createAuthenticateMessage(new Account(username, pwd));
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}

	private static long testAddKeychain(RequestHandler rh, Account account, String name, String data, boolean expected){
		System.out.printf("Adding keychain to %s: (%s, %s)%n", account, name, data);
		String req = RequestMessenger.createAddKeychainsMessage(account, name, data);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
		return js.getJSONObject("keychain").getLong("directoryEntryId");
	}

	private static void testUpdateKeychain(RequestHandler rh, Account account, long id, String name, String encryptKey, boolean expected){
		System.out.printf("Updating keychain to %s (%d,%s,%s)%n", account, id, name, encryptKey);
		String req = RequestMessenger.createUpdateKeychainMessage(account, id, name, encryptKey);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertThat(js.getString("status").equals("success"), is(expected));
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

	private static void testRemoveKeychain(RequestHandler rh, Account account, long id, boolean expected){
		System.out.printf("Removing keychain %d from %s%n", id, account);
		String req = RequestMessenger.createRemoveKeychainMessage(account, id);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertThat(js.getString("status").equals("success"), is(expected));
	}

	public static void testCreateAccount(RequestHandler rh, Account account, String email) {
		String req = RequestMessenger.createAddUserMessage(account, email, "", "", "");
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertThat(js.getString("status").equals("success"), is(true));
	}

	public static long testShareKeychain(RequestHandler rh, Account owner, Account shared, boolean canWrite, long id) {
		String req = RequestMessenger.createSharedKeychainMessage(owner, id, shared.getUsername(), canWrite);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js1 = new JSONObject(res);

		assertThat(js1.getString("status").equals("success"), is(true));
		long sharedid = js1.getJSONObject(typeToString(MessagePackager.Asset.SHARED_KEYCHAIN)).getLong("directoryEntryId");
		System.out.println("Test sharing keychain was successful.");

		req = RequestMessenger.createGetKeychainMessage(shared, sharedid);
		res = rh.handleRequest(req);
		JsonObject js2 = Json.createReader(new StringReader(res)).readObject();

		assertThat(js2.getString("status").equals("success"), is(true));
		ServerKeychain sharedView = new ServerKeychain(js2.getJsonObject("keychain"));
		assertThat(sharedView.getOwnerUsername(), is(owner.getUsername()));
		assertThat(sharedView.getSharedUsername(), is(shared.getUsername()));
		assertThat(sharedView.getOwnerDirectoryEntryId(), is(id));
		assertThat(sharedView.getDirectoryEntryId(), is(sharedid));
		System.out.println("Test shared getting keychain was successful");

		req = RequestMessenger.createGetKeychainMessage(owner, sharedid);
		res = rh.handleRequest(req);
		JsonObject js3 = Json.createReader(new StringReader(res)).readObject();

		assertThat(js3.getString("status").equals("success"), is(true));
		ServerKeychain ownerView = new ServerKeychain(js3.getJsonObject("keychain"));
		assertThat(ownerView.getOwnerUsername(), is(owner.getUsername()));
		assertThat(ownerView.getSharedUsername(), is(shared.getUsername()));
		assertThat(ownerView.getOwnerDirectoryEntryId(), is(id));
		assertThat(ownerView.getDirectoryEntryId(), is(sharedid));
		System.out.println("Test owner getting keychain was successful");

		req = RequestMessenger.createGetKeychainMessage(shared, id);
		res = rh.handleRequest(req);
		JSONObject js4 = new JSONObject(res);
		assertThat(js4.getString("status").equals("success"), is(false));

		req = RequestMessenger.createGetKeychainMessage(owner, id);
		res = rh.handleRequest(req);
		JSONObject js5 = new JSONObject(res);
		assertThat(js5.getString("status").equals("success"), is(true));

		return sharedid;
	}

	public static void testGetSharedKeychains(RequestHandler rh, Account owner, Account shared1, Account shared2, long id) {
		String req = RequestMessenger.createSharedKeychainMessage(owner, id, shared1.getUsername(), true);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js1 = new JSONObject(res);

		assertThat(js1.getString("status").equals("success"), is(true));
		long sharedid1 = js1.getJSONObject(typeToString(MessagePackager.Asset.SHARED_KEYCHAIN)).getLong("directoryEntryId");
		System.out.println("Test sharing keychain was successful.");

		req = RequestMessenger.createSharedKeychainMessage(owner, id, shared2.getUsername(), true);
		res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js2 = new JSONObject(res);

		assertThat(js2.getString("status").equals("success"), is(true));
		long sharedid2 = js2.getJSONObject(typeToString(MessagePackager.Asset.SHARED_KEYCHAIN)).getLong("directoryEntryId");
		System.out.println("Test sharing keychain was successful.");

		req = RequestMessenger.createGetKeychainInstancesMessage(owner, id);
		res = rh.handleRequest(req);
		JsonObject js3 = Json.createReader(new StringReader(res)).readObject();

		System.out.println("Got response:" + res);

		assertThat(js3.getString("status").equals("success"), is(true));
		JsonArray keychains = js3.getJsonArray(typeToString(MessagePackager.Asset.SHARED_KEYCHAINS));

		assertThat(keychains, notNullValue());
		assertThat(keychains.size(), is(3));

		for (int i = 0; i < keychains.size(); i++) {
			ServerKeychain sharedKeychain = new ServerKeychain(keychains.getJsonObject(i));
			assertThat(sharedKeychain.getDirectoryEntryId(), anyOf(is(id), is(sharedid1), is(sharedid2)));
		}
	}

	@Before
	public void setUp() throws Exception {
		assertThat(Crypto.init(false), is(true));

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

		Account account = new Account("ning", "5431");

		testGetKeychains(rh, account, 2);
		long fbId = testAddKeychain(rh, account, "fb", "1234", true);
		testUpdateKeychain(rh, account, fbId, "lala", "4321", true);
		testUpdateKeychain(rh, account, -1, "lala", "4321", false);
 		testGetKeychains(rh, account, 3);
		
		testRemoveKeychain(rh, account, -1, false);
		testRemoveKeychain(rh, account, fbId, true);
		testGetKeychains(rh, account, 2);

		Account account1 = new Account("test1", "password");
		Account account2 = new Account("test2", "password");
		Account account3 = new Account("test3", "password");

		testCreateAccount(rh, account1, "a@b.com");
		testCreateAccount(rh, account2, "b@b.com");

		long testkcId1 = testAddKeychain(rh, account1, "testkc", "1234", true);

		testShareKeychain(rh, account1, account2, true, testkcId1);

		long testkcId2 = testAddKeychain(rh, account1, "testkc2", "12345", true);

		testGetSharedKeychains(rh, account1, account2, account3, testkcId2);
	}

}
