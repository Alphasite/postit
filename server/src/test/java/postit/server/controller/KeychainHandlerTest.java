package postit.server.controller;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import postit.server.database.Database;
import postit.server.database.TestH2;
import postit.server.model.ServerKeychain;

import java.util.List;

import static org.hamcrest.core.Is.is;
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

	@Before
	public void setUp() throws Exception {
		database = new TestH2();
		db = new DatabaseController(database);
		//ah = new AccountHandler(db, new SecureRandom());
		kh = new KeychainHandler(db);

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
	
	public static void testRemoveKeychain(KeychainHandler kh, int directoryEntryId, boolean expected){
		boolean res = kh.removeKeychain(directoryEntryId);
		System.out.printf("Removing keychain %d %s%n", directoryEntryId, res ? "successful" : "failed");
		assertTrue(res == expected);
	}	
	
	@Test
	public void runTest(){

		
		String username = "mc";
		//boolean res = ah.addAccount(username, "cs5431", "mc@cornell.edu", "m", "c");

		int id1 = testAddKeychain(kh, username, "netflix", true);
		testUpdateKeychain(kh, username, id1, null, "test1", true);
		int id2 = testAddKeychain(kh, username, "fb", true);
		testRemoveKeychain(kh, id2, true);

		List<ServerKeychain> list = kh.getKeychains(username);
		assertEquals(1, list.size());
		System.out.println(list);
		
		testRemoveKeychain(kh, id1, true);
		list = kh.getKeychains(username);
		assertEquals(list.size(), 0);
		System.out.println(kh.getKeychains(username));
		
		db.removeAccount(username);
	}
}
