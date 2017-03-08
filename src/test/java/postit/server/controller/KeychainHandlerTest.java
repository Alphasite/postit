package postit.server.controller;

import org.json.JSONObject;

/**
 * 
 * @author Ning
 *
 */
public class KeychainHandlerTest {

	public static int testAddKeychain(KeychainHandler kh, String username, String name, String pwd){
		JSONObject js = kh.createKeychain(username, name, ".", "123456");
		boolean res = js.getString("status").equals("success");
		System.out.printf("Adding keychain to %s: (%s, %s) %s\n", username, name, pwd, res ? "successful" : "failed");
		return js.getInt("directoryEntryId");
	}
	
	public static void testUpdateKeychain(KeychainHandler kh, int directoryEntryId, String name, 
			String encryptKey, String password, String metadata){
		boolean res = kh.updateKeychain(directoryEntryId, name, encryptKey, password, metadata);
		System.out.printf("Updating keychain %d (%s,%s,%s,%s) %s\n", directoryEntryId, name, encryptKey, password, metadata, res ? "successful" : "failed");
	}
	
	public static void testRemoveKeychain(KeychainHandler kh, int directoryEntryId){
		boolean res = kh.removeKeychain(directoryEntryId);
		System.out.printf("Removing keychain %d %s\n", directoryEntryId, res ? "successful" : "failed");
	}
	
	public static void main(String[] args){
		DatabaseController db = new DatabaseController();
		AccountHandler ah = new AccountHandler(db);
		KeychainHandler kh = new KeychainHandler(db);
		
		String username = "mc";
		boolean res = ah.addAccount(username, "cs5431", "mc@cornell.edu", "m", "c");
		int dirId = db.getDirectory(username).getDirectoryId();
		System.out.println("directoryId: " + dirId);
		
		int id1 = testAddKeychain(kh, username, "netflix", "password");
		testUpdateKeychain(kh, id1, null, null, "netflixpwd", null);
		int id2 = testAddKeychain(kh, username, "fb", "123456"); 
		testRemoveKeychain(kh, id2);
		
		System.out.println(kh.getKeychains(username));
		
		testRemoveKeychain(kh, id1);
		System.out.println(kh.getKeychains(username));
		
		db.removeAccount(username);
		
	}
}
