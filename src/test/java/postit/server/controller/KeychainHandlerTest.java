package postit.server.controller;

import javax.json.JsonObject;

/**
 * 
 * @author Ning
 *
 */
public class KeychainHandlerTest {

	public static int testAddKeychain(DatabaseController db, KeychainHandler kh, String username, String name, String pwd){
		JsonObject js = kh.createKeychain(db, username, name, ".", "123456");
		boolean res = js.getString("status").equals("success");
		System.out.printf("Adding keychain to %s: (%s, %s) %s\n", username, name, pwd, res ? "successful" : "failed");
		return js.getInt("directory_entry_id");
	}
	
	public static void testUpdateKeychain(DatabaseController db, KeychainHandler kh, int directoryEntryId, String name, 
			String encryptKey, String password, String metadata){
		boolean res = kh.updateKeychain(db, directoryEntryId, name, encryptKey, password, metadata);
		System.out.printf("Updating keychain %d (%s,%s,%s,%s) %s\n", directoryEntryId, name, encryptKey, password, metadata, res ? "successful" : "failed");
	}
	
	public static void testRemoveKeychain(DatabaseController db, KeychainHandler kh, int directoryEntryId){
		boolean res = kh.removeKeychain(db, directoryEntryId);
		System.out.printf("Removing keychain %d %s\n", directoryEntryId, res ? "successful" : "failed");
	}
	
	public static void main(String[] args){
		DatabaseController db = new DatabaseController();
		AccountHandler ah = new AccountHandler();
		KeychainHandler kh = new KeychainHandler();
		
		String username = "mc";
		boolean res = ah.addAccount(db, username, "cs5431", "mc@cornell.edu", "m", "c");
		int dirId = db.getDirectory(username).getDirectoryId();
		System.out.println("directoryId: " + dirId);
		
		int id1 = testAddKeychain(db, kh, username, "netflix", "password");
		testUpdateKeychain(db, kh, id1, null, null, "netflixpwd", null);
		int id2 = testAddKeychain(db, kh, username, "fb", "123456"); 
		testRemoveKeychain(db, kh, id2);
		
		System.out.println(kh.getKeychains(db, username));
		
		testRemoveKeychain(db, kh, id1);
		System.out.println(kh.getKeychains(db, username));
		
		db.removeAccount(username);
		
	}
}
