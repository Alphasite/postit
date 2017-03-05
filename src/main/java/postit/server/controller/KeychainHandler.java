package postit.server.controller;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import postit.server.model.*;

/**
 * Class handling requests from frontend and directs to the proper backend controller.
 * Changes needed in future:
 * - change metadata's type
 * - an interface for Handler
 * - event response JSONObject or CODE instead of boolean
 * - privatize the handling functions
 *
 * @author Ning
 */
public class KeychainHandler {
	
    public JsonObject createKeychain(DatabaseController db, String username, String name, String path, String pwd) {
        Directory dir = db.getDirectory(username);
    	if (dir != null){
    		JsonObject de = db.addDirectoryEntry(name, "", dir.getDirectoryId()); // change to generated encryptionkey
    		if (de.getString("status").equals("success")){
    			JsonObject kc = db.addKeychain(de.getInt("directory_entry_id"), pwd, ""); // add metadata 
    			if (kc.getString("status").equals("success"))
    				return de;
    		}
    	}
        return Json.createObjectBuilder().add("status", "failure").build();
    }

    /**
     * Updates keychain information in the database. Any item that should be left unchanged should be null.
     * @param db
     * @param directoryEntryId
     * @param name
     * @param encryptKey
     * @param password
     * @param metadata
     * @return
     */
    public boolean updateKeychain(DatabaseController db, int directoryEntryId, String name, String encryptKey, String password, String metadata) {
    	DirectoryEntry de = db.getDirectoryEntry(directoryEntryId);
    	if (name != null) de.setName(name); 
    	if (encryptKey != null) de.setEncryptionKey(encryptKey); 
    	if (db.updateDirectoryEntry(de)){
    		Keychain k = db.getKeychain(directoryEntryId);
    		if (password != null) k.setPassword(password); 
    		if (metadata != null) k.setMetadata(metadata); 
    		return db.updateKeychain(k);
    	}
    	return false;
    }

    public boolean removeKeychain(DatabaseController db, int directoryEntryId) {
    	if (db.removeDirectoryEntry(directoryEntryId)) 
    		return db.removeKeychain(directoryEntryId);
        return false;
    }
	
    public JsonObject getKeychains(DatabaseController db, String username) {
    	//Assuming authentication
    	
        Directory dir = db.getDirectory(username);
        if (dir == null) return Json.createObjectBuilder().add("status", "failure").build();
        
        List<DirectoryEntry> des = db.getDirectoryEntries(dir.getDirectoryId());
        JsonArrayBuilder build = Json.createArrayBuilder();
        //TODO double check the format of output needed from client side
        for (DirectoryEntry de: des){
        	Keychain k = db.getKeychain(de.getDirectoryEntryId());
        	if (k == null)
        		build.add(Json.createObjectBuilder().add("status", "failure"));
        	else
        		build.add(Json.createObjectBuilder()
        				.add("status", "success")
        				.add("directory_entry_id", de.getDirectoryEntryId())
        				.add("name", de.getName()) 
        				.add("encryption_key", de.getEncryptionKey()) 
        				.add("password", k.getPassword()) 
        				.add("metadata", k.getMetadata()));
        }
        return Json.createObjectBuilder()
        		.add("status", "success")
        		.add("keychains", build)
        		.build();
    }
}
