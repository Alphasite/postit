package postit.client.handler;

import postit.shared.model.*;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.*;

public class RequestMessenger {

	public static String createAuthenticateMessage(String username, String password){
	
		Account account = new Account();
		account.setUsername(username);
		account.setPassword(password);
		return MessagePackager.createRequest(Action.AUTHENTICATE, username, Asset.ACCOUNT, account);
	}
	
	public static String createGetDirectoryMessage(String username){
	
		return "";
	}
	
	public static String createUpdateDirectoryMessage(String username){
		
		return "";
	}
	
	public static String createAddKeychainsMessage(String username, String name, String encryptionKey, String password, String meta){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		dak.setEncryptionKey(encryptionKey);
		dak.setPassword(password);
		dak.setMetadata(meta);
		return MessagePackager.createRequest(Action.ADD, username, Asset.KEYCHAIN, dak);
	}
	
	public static String createGetKeychainsMessage(String username){
		return MessagePackager.createRequest(Action.GET, username, Asset.KEYCHAINS, null);
	}
	
	public static String createGetKeychainMessage(String username, String name){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		return MessagePackager.createRequest(Action.GET, username, Asset.KEYCHAIN, dak);
	}
	
	public static String createRemoveKeychainMessage(String username, String name){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		return MessagePackager.createRequest(Action.REMOVE, username, Asset.KEYCHAIN, dak);
	}
	
	public static String createUpdateKeychainMessage(String username, String name, String encryptionKey, String password, String meta){
		// put any unchanged field as null
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		dak.setEncryptionKey(encryptionKey);
		dak.setPassword(password);
		dak.setMetadata(meta);
		return MessagePackager.createRequest(Action.UPDATE, username, Asset.KEYCHAIN, dak);
	}
}
