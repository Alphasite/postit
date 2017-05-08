package postit.server.controller;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import postit.server.database.Database;
import postit.server.database.TestH2;
import postit.server.model.ServerKeychain;
import postit.shared.Crypto;

import java.util.List;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

/**
 * 
 * @author Ning
 *
 */
public class KeychainHandlerTest {
	Database database;
	DatabaseController db;
	//AccountHandler ah;
	KeychainHandler kh;
	AccountHandler ah;

	@Before
	public void setUp() throws Exception {
		Crypto.init(false);

		database = new TestH2();
		db = new DatabaseController(database);
		kh = new KeychainHandler(db);
		ah = new AccountHandler(db, Crypto.getRandom());

		assertThat(database.initDatabase(), is(true));
	}

	public static int testAddKeychain(KeychainHandler kh, String username, String name, boolean expected){
		JSONObject js = kh.createKeychain(username, name);
		boolean res = js.getString("status").equals("success");
		System.out.printf("Adding keychain for %s: (%s) %s%n", username, name, res ? "successful" : "failed");
		assertTrue(js.getString("status").equals("success") == expected);
		
		return js.getString("status").equals("failure") ? -1 : js.getInt("directoryEntryId");
	}
	
	public static void testUpdateKeychain(KeychainHandler kh, String username, int directoryEntryId, String name, String data, boolean expected){
		boolean res = kh.updateKeychain(username, directoryEntryId, name, data);
		System.out.printf("Updating keychain %d (%s,%s,%s) %s%n", directoryEntryId, username, name, data, res ? "successful" : "failed");
		assertTrue(res == expected);
	}
	
	public static void testRemoveKeychain(KeychainHandler kh, String username, int directoryEntryId, boolean expected){
		boolean res = kh.removeKeychain(username, directoryEntryId);
		System.out.printf("Removing keychain %d %s%n", directoryEntryId, res ? "successful" : "failed");
		assertTrue(res == expected);
	}

	public static long testShareKeychain(KeychainHandler kh, String owner, String shared, boolean canWrite, long id) {
		JSONObject js = kh.shareKeychain(owner, shared, canWrite, id);
		assertThat(js.getString("status").equals("success"), is(true));
		long sharedid = js.getLong("directoryEntryId");
		System.out.println("Test sharing keychain was successful.");

		ServerKeychain sharedResult = kh.getKeychain(shared, sharedid);
		assertThat(sharedResult, notNullValue());
		assertThat(sharedResult.getOwnerUsername(), is(owner));
		assertThat(sharedResult.getSharedUsername(), is(shared));
		assertThat(sharedResult.getOwnerDirectoryEntryId(), is(id));
		System.out.println("Test shared getting keychain was successful");

		ServerKeychain ownerView = kh.getKeychain(owner, sharedid);
		assertThat(ownerView, notNullValue());
		assertThat(ownerView.getOwnerUsername(), is(owner));
		assertThat(ownerView.getSharedUsername(), is(shared));
		assertThat(ownerView.getOwnerDirectoryEntryId(), is(id));
		System.out.println("Test owner getting keychain was successful");

		assertThat(kh.getKeychain(shared, id), nullValue());

		return sharedid;
	}

	public static void testGetSharedKeychains(KeychainHandler kh, String owner, String shared1, String shared2, long id) {
		JSONObject js = kh.shareKeychain(owner, shared1, true, id);
		assertThat(js.getString("status").equals("success"), is(true));
		long sharedid1 = js.getLong("directoryEntryId");
		System.out.println("Test sharing1 keychain was successful: " + shared1);

		js = kh.shareKeychain(owner, shared2, true, id);
		assertThat(js.getString("status").equals("success"), is(true));
		long sharedid2 = js.getLong("directoryEntryId");
		System.out.println("Test sharing2 keychain was successful: " + shared2);

		List<ServerKeychain> sharedKeychains = kh.getSharedKeychains(owner, id);
		assertThat(sharedKeychains, notNullValue());
		assertThat(sharedKeychains.size(), is(2));

		for (ServerKeychain sharedKeychain : sharedKeychains) {
			assertThat(sharedKeychain.getDirectoryEntryId(), anyOf(is(sharedid1), is(sharedid2)));
		}
	}

	public static void testGetOwnerKeychain(KeychainHandler kh, String owner, String shared, long id) {
		JSONObject js = kh.shareKeychain(owner, shared, true, id);
		assertThat(js.getString("status").equals("success"), is(true));
		long sharedid = js.getLong("directoryEntryId");
		System.out.println("Test sharing keychain was successful: " + shared);

		ServerKeychain sharedKeychain = kh.getOwnersKeychain(shared, sharedid);
		assertThat(sharedKeychain, notNullValue());
		assertThat(sharedKeychain.getDirectoryEntryId(), is(id));

		sharedKeychain = kh.getOwnersKeychain(owner, id);
		assertThat(sharedKeychain, notNullValue());
		assertThat(sharedKeychain.getDirectoryEntryId(), is(id));
	}
	
	@Test
	public void runTest(){

		
		String username = "mc";

		assertThat(ah.addAccount("test1", "cs5431", "test1@cornell.edu", "m", "c","keypair", "8000000000"), is(true));
		assertThat(ah.addAccount("test2", "cs5431", "test2@cornell.edu", "m", "c","keypair", "8000000000"), is(true));
		assertThat(ah.addAccount("test3", "cs5431", "test3@cornell.edu", "m", "c","keypair", "8000000000"), is(true));

		int id1 = testAddKeychain(kh, username, "netflix", true);
		testUpdateKeychain(kh, username, id1, null, "test1", true);
		int id2 = testAddKeychain(kh, username, "fb", true);
		testRemoveKeychain(kh, username, id2, true);

		List<ServerKeychain> list = kh.getKeychains(username);
		assertEquals(1, list.size());
		System.out.println(list);
		
		testRemoveKeychain(kh, username, id1, true);
		list = kh.getKeychains(username);
		assertEquals(list.size(), 0);
		System.out.println(kh.getKeychains(username));

		int id3 = testAddKeychain(kh, "test1", "banana", true);
		testShareKeychain(kh, "test1", "test2", true, id3);

		int id4 = testAddKeychain(kh, "test1", "banana", true);
		testGetSharedKeychains(kh, "test1", "test2", "test3", id4);

		int id5 = testAddKeychain(kh, "test1", "banana12", true);
		testGetOwnerKeychain(kh, "test1", "test2", id5);

		db.removeAccount(username);
	}
}
