package postit.client.controller;

import postit.client.keychain.Account;
import postit.shared.model.*;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.*;

public class RequestMessenger {

	public static String createAuthenticateMessage(Account clientAccount){
		postit.server.model.Account account = new postit.server.model.Account();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		return MessagePackager.createRequest(Action.AUTHENTICATE, clientAccount, Asset.ACCOUNT, account);
	}
	
	public static String createAddUserMessage(Account clientAccount, String email, String firstname, String lastname){
		postit.server.model.Account account = new postit.server.model.Account();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		account.setEmail(email);
		account.setFirstname(firstname);
		account.setLastname(lastname);
		return MessagePackager.createRequest(Action.ADD, null, Asset.ACCOUNT, account);
	}
	
	public static String createRemoveUserMessage(Account clientAccount){
		postit.server.model.Account account = new postit.server.model.Account();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		return MessagePackager.createRequest(Action.REMOVE, null, Asset.ACCOUNT, account);
	}
	
	public static String createGetDirectoryMessage(Account account){
	
		return "";
	}
	
	public static String createUpdateDirectoryMessage(Account account){
		
		return "";
	}
	
	public static String createAddKeychainsMessage(Account account, String name, String encryptionKey, String password, String meta){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		dak.setEncryptionKey(encryptionKey);
		dak.setPassword(password);
		dak.setMetadata(meta);
		return MessagePackager.createRequest(Action.ADD, account, Asset.KEYCHAIN, dak);
	}
	
	public static String createGetKeychainsMessage(Account account){
		return MessagePackager.createRequest(Action.GET, account, Asset.KEYCHAINS, null);
	}
	
	public static String createGetKeychainMessage(Account account, String name){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		return MessagePackager.createRequest(Action.GET, account, Asset.KEYCHAIN, dak);
	}
	
	public static String createGetKeychainMessage(Account account, long keychainId){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setDirectoryEntryId((int) keychainId);
		return MessagePackager.createRequest(Action.GET, account, Asset.KEYCHAIN, dak);
	}
	
	public static String createRemoveKeychainMessage(Account account, String name){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		return MessagePackager.createRequest(Action.REMOVE, account, Asset.KEYCHAIN, dak);
	}
	
	public static String createRemoveKeychainMessage(Account account, long keychainId){
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setDirectoryEntryId((int) keychainId);
		return MessagePackager.createRequest(Action.REMOVE, account, Asset.KEYCHAIN, dak);
	}
	
	public static String createUpdateKeychainMessage(Account account, String name, String encryptionKey, String password, String meta) {
		// put any unchanged field as null
		DirectoryAndKey dak = new DirectoryAndKey();
		dak.setName(name);
		dak.setEncryptionKey(encryptionKey);
		dak.setPassword(password);
		dak.setMetadata(meta);
		return MessagePackager.createRequest(Action.UPDATE, account, Asset.KEYCHAIN, dak);
	}
}
