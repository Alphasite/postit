package postit.server.controller;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;

import postit.server.database.Database;
import postit.server.database.MySQL;
import postit.server.model.Account;
import postit.shared.MessagePackager;
import postit.shared.MessagePackager.*;
import postit.shared.model.DirectoryAndKey;

/**
 * 
 * @author Ning
 *
 */
public class RequestHandler {
	
	private AccountHandler ah;
	private KeychainHandler kh;
	private SecureRandom rand;
	
	public RequestHandler(Database database) throws ExceptionInInitializerError {
		DatabaseController db = new DatabaseController(database);
		ah = new AccountHandler(db, rand);
		kh = new KeychainHandler(db);
		rand = new SecureRandom();
	}

	/**
	 * Takes in request, process the request, and outputs the proper response
	 * @param request
	 * @return
	 */
	public String handleRequest(String request){
		//TODO refactor this gigantic thing using multiple engine classes that handle requests
		// associated to specific assets
		
		JSONObject json = new JSONObject(request);
		
		Action act = Action.valueOf(json.getString("action"));
		Asset asset = Asset.valueOf(json.getString("asset"));
		String username = json.getString("username"); // only empty for ADD ACCOUNT
		if (username.equals("") && act != Action.ADD && asset != Asset.ACCOUNT)
			return MessagePackager.createResponse(false, "", "Missing username as input", asset, null);
		
		String assetName = MessagePackager.typeToString(asset).toLowerCase();
		JSONObject obj = null;
		if (json.has(assetName)) 
			obj = json.getJSONObject(assetName);

		switch(act){
		case ADD:
			switch(asset){
			case ACCOUNT:
				Account account = new Account(obj.getString("username"), obj.getString("password"), obj.getString("email"),
						obj.getString("firstname"), obj.getString("lastname")); 
				if (ah.addAccount(account)) {
					return MessagePackager.createResponse(true, account.getUsername(), "", asset, account); 
				}
				else
					return MessagePackager.createResponse(false, "", "Failed to create new account", asset, null);
			case KEYCHAIN:
				DirectoryAndKey dak = new DirectoryAndKey(obj.getInt("directoryEntryId"), obj.getString("name"), 
						obj.getString("encryptionKey"), obj.getInt("directoryId"), obj.getString("password"), obj.getString("metadata"));
				JSONObject js = kh.createKeychain(username, dak);
				if (js.getString("status").equals("success")){
					dak.setDirectoryEntryId(js.getInt("directoryEntryId")); 
					dak.setDirectoryId(js.getInt("directoryId"));
					return MessagePackager.createResponse(true, username, "", asset, dak);
				}
				else 
					return js.toString();
			default:
				break;
			}
		case AUTHENTICATE:
			switch(asset){
			case ACCOUNT:
				boolean success = ah.authenticate(obj.getString("username"), obj.getString("password"));
				return MessagePackager.createResponse(success, username, success ? "" : "Incorrect login information.", asset, null);
			default:
				break;
			}
		case GET:
			switch(asset){
			case ACCOUNT:
				if (! username.equals(obj.getString("username")))
					return MessagePackager.createResponse(false, username, "Account information has wrong username", asset, null);
				Account account = ah.getAccount(username);
				if (account != null)
					return MessagePackager.createResponse(true, username, "", asset, account);
				else
					return MessagePackager.createResponse(false, username, "Unable to get account information of " + username, asset, null);
			case KEYCHAIN:
				int deId = obj.getInt("directoryEntryId");
				DirectoryAndKey dak;
				if (deId != -1) dak = kh.getKeychain(deId);
				else dak = kh.getKeychain(username, obj.getString("name"));
				if (dak != null)
					return MessagePackager.createResponse(true, username, "", asset, dak);
				else
					return MessagePackager.createResponse(false, username, "Unable to get keychain information of " + deId, asset, null);
			case KEYCHAINS:
				List<DirectoryAndKey> list = kh.getKeychains(username);
				if (list != null)
					return MessagePackager.createResponse(true, username, "", asset, list);
				else
					return MessagePackager.createResponse(false, username, "Unable to get keychains of " + username, asset, null);
			default:
				break;
			}
			break;
		case REMOVE:
			switch(asset){
			case ACCOUNT:
				if (! username.equals(obj.getString("username")))
					return MessagePackager.createResponse(false, username, "Account information has wrong username", asset, null);
				if (ah.removeAccount(username, obj.getString("password"))) 
					return MessagePackager.createResponse(true, username, "", asset, null);
				else
					return MessagePackager.createResponse(false, username, "Unable to remove account " + username, asset, null);
			case KEYCHAIN:
				int deId;
				if (! obj.has("directoryEntryId") || obj.getInt("directoryEntryId") == -1){
					DirectoryAndKey dak = kh.getKeychain(username, obj.getString("name"));
					if (dak == null)
						return MessagePackager.createResponse(false, username, "No keychain has name " + obj.getString("name"), asset, null);
					deId = dak.getDirectoryEntryId();
				}
				else
					deId = obj.getInt("directoryEntryId");
				if (kh.removeKeychain(deId))
					return MessagePackager.createResponse(true, username, "", asset, null);
				else
					return MessagePackager.createResponse(false, username, "Unable to remove keychain " + deId, asset, null);
			default:
				break;
			}
		case UPDATE:
			switch(asset){
			case ACCOUNT:
				Account account = new Account(obj.getString("username"), obj.getString("password"), obj.getString("email"),
						obj.getString("firstname"), obj.getString("lastname"));
				if (ah.updateAccount(account)) 
					return MessagePackager.createResponse(true, username, "", asset, account);
				else
					return MessagePackager.createResponse(false, username, "Unable to update account information of " + account.getUsername(), 
							asset, null);
			case KEYCHAIN:
				DirectoryAndKey dak = new DirectoryAndKey();
				if (obj.has("directoryId")) dak.setDirectoryId(obj.getInt("directoryId"));
				if (obj.has("directoryEntryId")) dak.setDirectoryEntryId(obj.getInt("directoryEntryId"));
				if (obj.has("name")) dak.setName(obj.getString("name"));
				if (obj.has("encryptionKey")) dak.setEncryptionKey(obj.getString("encryptionKey"));
				if (obj.has("password")) dak.setPassword(obj.getString("password"));
				if (obj.has("metadata")) dak.setMetadata(obj.getString("metadata"));
				
				if (dak.getDirectoryId() == -1){
					if (dak.getDirectoryEntryId() == -1){
						DirectoryAndKey dak2 = kh.getKeychain(username, obj.getString("name"));
						if (dak2 == null)
							return MessagePackager.createResponse(false, username, "No keychain has name " + obj.getString("name"), asset, dak);
						dak.setDirectoryEntryId(dak2.getDirectoryEntryId());
					}
					if (! kh.updateKeychain(dak))
						return MessagePackager.createResponse(false, username, "Unable to update keychain information of " + dak.getName(), asset, dak);
					return MessagePackager.createResponse(true, username, "", asset, dak);
				}
				else if (kh.updateKeychain(dak)) 
					return MessagePackager.createResponse(true, username, "", asset, dak);
				else 
					return MessagePackager.createResponse(false, username, "Unable to update keychain information of " + dak.getName(), 
							asset, null);
			default:
				break;
			}
		default:
			break;
		}
		
		return MessagePackager.createResponse(false, username, String.format("Invalid parameters: (%s, %s)", act, asset), null, null);
	}
}
