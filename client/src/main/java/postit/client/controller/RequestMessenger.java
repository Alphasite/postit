package postit.client.controller;

import postit.client.keychain.Account;
import postit.server.model.ServerAccount;
import postit.server.model.ServerKeychain;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.*;

import static postit.client.ClientMessagePackager.createRequest;

public class RequestMessenger {

	public static String createAuthenticateMessage(Account clientAccount){
		ServerAccount serverAccount = new ServerAccount();
		serverAccount.setUsername(clientAccount.getUsername());
		serverAccount.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		return createRequest(Action.AUTHENTICATE, clientAccount, Asset.ACCOUNT, serverAccount);
	}
	
	public static String createAddUserMessage(Account clientAccount, String email, String firstname, String lastname){
		ServerAccount serverAccount = new ServerAccount();
		serverAccount.setUsername(clientAccount.getUsername());
		serverAccount.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		serverAccount.setEmail(email);
		serverAccount.setFirstname(firstname);
		serverAccount.setLastname(lastname);
		return createRequest(Action.ADD, null, Asset.ACCOUNT, serverAccount);
	}
	
	public static String createRemoveUserMessage(Account clientAccount){
		ServerAccount account = new ServerAccount();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(new String(clientAccount.getSecretKey().getEncoded()));
		return createRequest(Action.REMOVE, null, Asset.ACCOUNT, account);
	}
	
	public static String createGetDirectoryMessage(Account account){
	
		return "";
	}
	
	public static String createUpdateDirectoryMessage(Account account){
		
		return "";
	}
	
	public static String createAddKeychainsMessage(Account account, String name, String data){
		ServerKeychain keychain = new ServerKeychain();
		keychain.setName(name);
		keychain.setOwnerUsername(account.getUsername());
		keychain.setData(data);
		return createRequest(Action.ADD, account, Asset.KEYCHAIN, keychain);
	}
	
	public static String createGetKeychainsMessage(Account account){
		return createRequest(Action.GET, account, Asset.KEYCHAINS, null);
	}
	
	public static String createGetKeychainMessage(Account account, long keychainId){
		ServerKeychain keychain = new ServerKeychain();
		keychain.setDirectoryEntryId((int) keychainId);
		return createRequest(Action.GET, account, Asset.KEYCHAIN, keychain);
	}
	
	public static String createRemoveKeychainMessage(Account account, long keychainId){
		ServerKeychain keychain = new ServerKeychain();
		keychain.setDirectoryEntryId((int) keychainId);
		return createRequest(Action.REMOVE, account, Asset.KEYCHAIN, keychain);
	}
	
	public static String createUpdateKeychainMessage(Account account, long serverId, String name, String data) {
		// put any unchanged field as null
		ServerKeychain keychain = new ServerKeychain();
		keychain.setName(name);
		keychain.setOwnerUsername(account.getUsername());
		keychain.setDirectoryEntryId(serverId);
		keychain.setData(data);
		return createRequest(Action.UPDATE, account, Asset.KEYCHAIN, keychain);
	}
}
