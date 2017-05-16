package postit.client.controller;

import postit.client.keychain.Account;
import postit.server.model.ServerAccount;
import postit.server.model.ServerKeychain;
import postit.shared.MessagePackager.Action;
import postit.shared.MessagePackager.Asset;

import java.nio.charset.StandardCharsets;

import static postit.client.ClientMessagePackager.createRequest;

public class RequestMessenger {

	public static String createAuthenticateMessage(Account clientAccount){
		ServerAccount serverAccount = new ServerAccount();
		serverAccount.setUsername(clientAccount.getUsername());
		serverAccount.setPassword(new String(clientAccount.getSecretKey().getEncoded(),StandardCharsets.UTF_8));
		return createRequest(Action.AUTHENTICATE, clientAccount, Asset.ACCOUNT, serverAccount);
	}
	
	public static String createAddUserMessage(Account clientAccount, String email, String firstname, String lastname, String phoneNumber, String keypair, String publickey){
		ServerAccount serverAccount = new ServerAccount();
		serverAccount.setUsername(clientAccount.getUsername());
		serverAccount.setPassword(new String(clientAccount.getSecretKey().getEncoded(),StandardCharsets.UTF_8));
		serverAccount.setEmail(email);
		serverAccount.setFirstname(firstname);
		serverAccount.setLastname(lastname);
		serverAccount.setPhoneNumber(phoneNumber);
		serverAccount.setKeypair(keypair);
		serverAccount.setPublickey(publickey);
		return createRequest(Action.ADD, null, Asset.ACCOUNT, serverAccount);
	}
	
	public static String createRemoveUserMessage(Account clientAccount){
		ServerAccount account = new ServerAccount();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(new String(clientAccount.getSecretKey().getEncoded(),StandardCharsets.UTF_8));
		return createRequest(Action.REMOVE, clientAccount, Asset.ACCOUNT, account);
	}

	public static String createGetKeypairMessage(Account clientAccount) {
		return createRequest(Action.GET, clientAccount, Asset.KEYPAIR, null);
	}

	public static String sendOtpMessage(Account clientAccount, String otp) {
		return createRequest(Action.AUTHENTICATE, clientAccount, Asset.KEYPAIR, otp);
	}

	public static String createGetUserMessage(Account clientAccount){
		ServerAccount account = new ServerAccount();
		account.setUsername(clientAccount.getUsername()); 
		return createRequest(Action.GET, clientAccount, Asset.ACCOUNT, account);
	}
	
	public static String createUpdateAccountMessage(Account clientAccount, String pwd, String email, String firstname, String lastname, String phoneNumber){
		ServerAccount account = new ServerAccount();
		account.setUsername(clientAccount.getUsername());
		account.setPassword(pwd);
		account.setEmail(email);
		account.setFirstname(firstname);
		account.setLastname(lastname);
		account.setPhoneNumber(phoneNumber);
		return createRequest(Action.UPDATE, clientAccount, Asset.ACCOUNT, account);
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

	public static String createSharedKeychainMessage(Account account, long serverId, String sharedUsername, boolean writeable) {
		ServerKeychain keychain = new ServerKeychain();
		keychain.setOwnerDirectoryEntryId(serverId);
		keychain.setSharedUsername(sharedUsername);
		keychain.setSharedHasWritePermission(writeable);
		return createRequest(Action.ADD, account, Asset.SHARED_KEYCHAIN, keychain);
	}

	public static String createUpdateSharedKeychainMessage(Account account, long serverId, String sharedUsername, boolean writeable) {
		ServerKeychain keychain = new ServerKeychain();
		keychain.setOwnerDirectoryEntryId(serverId);
		keychain.setSharedUsername(sharedUsername);
		keychain.setSharedHasWritePermission(writeable);
		return createRequest(Action.UPDATE, account, Asset.SHARED_KEYCHAIN, keychain);
	}

	public static String deleteSharedKeychainMessage(Account account, long serverId, String sharedUsername, boolean writeable) {
		ServerKeychain keychain = new ServerKeychain();
		keychain.setOwnerDirectoryEntryId(serverId);
		keychain.setSharedUsername(sharedUsername);
		keychain.setSharedHasWritePermission(writeable);
		return createRequest(Action.REMOVE, account, Asset.SHARED_KEYCHAIN, keychain);
	}

	public static String createGetKeychainInstancesMessage(Account account, long serverId){
		ServerKeychain keychain = new ServerKeychain();
		keychain.setOwnerUsername(account.getUsername());
		keychain.setOwnerDirectoryEntryId(serverId);
		return createRequest(Action.GET, account, Asset.SHARED_KEYCHAINS, keychain);
	}

	public static String createGetOwnerKeychainMessage(Account account, long serverId){
		ServerKeychain keychain = new ServerKeychain();
		keychain.setDirectoryEntryId(serverId);
		return createRequest(Action.GET, account, Asset.OWNER_KEYCHAIN, keychain);
	}
}
