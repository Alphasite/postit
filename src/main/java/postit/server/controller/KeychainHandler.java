package postit.server.controller;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import postit.server.model.*;
import postit.shared.model.DirectoryAndKey;

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
	
	private DatabaseController db;
	
	public KeychainHandler(DatabaseController db){
		this.db = db;
	}
	
    public JSONObject createKeychain(String username, String name, String path, String pwd) {
    	return createKeychain(username, new DirectoryAndKey(-1, name, "", -1, pwd, ""));
    }

    public JSONObject createKeychain(String username, DirectoryAndKey dak){
    	Directory dir = db.getDirectory(username);
    	if (dir != null){
    		JSONObject de = db.addDirectoryEntry(dak.getName(), dak.getEncryptionKey(), dir.getDirectoryId()); // change to generated encryptionkey
    		if (de.getString("status").equals("success")){
    			if (db.addKeychain(de.getInt("directoryEntryId"), dak.getPassword(), dak.getMetadata())){
    				de.put("directoryId", dir.getDirectoryId());
    				return de;
    			}
    		}
    	}
    	JSONObject res = new JSONObject();
    	res.put("status", "failure");
    	return res;
    }
    
    /**
     * Updates keychain information in the database. Any item that should be left unchanged should be null.
     * @param directoryEntryId
     * @param name
     * @param encryptKey
     * @param password
     * @param metadata
     * @return
     */
    public boolean updateKeychain(int directoryEntryId, String name, String encryptKey, String password, String metadata) {
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

    public boolean updateKeychain(DirectoryAndKey dak){
    	return updateKeychain(dak.getDirectoryEntryId(), dak.getName(), dak.getEncryptionKey(), dak.getPassword(), dak.getMetadata());
    }
    
    public boolean updateKeychain(String username, DirectoryAndKey dak){
    	Directory dir = db.getDirectory(username);
    	dak.setDirectoryId(dir.getDirectoryId());
    	return updateKeychain(dak);
    }
    
    public boolean removeKeychain(int directoryEntryId) {
    	if (db.removeDirectoryEntry(directoryEntryId)) 
    		return db.removeKeychain(directoryEntryId);
        return false;
    }
	
    public DirectoryAndKey getKeychain(int directoryEntryId){
    	DirectoryEntry de = db.getDirectoryEntry(directoryEntryId);
    	if (de == null) return null;
    	Keychain k = db.getKeychain(directoryEntryId);
    	if (k != null) return new DirectoryAndKey(de, k);
    	else return null;
    }
    
    public DirectoryAndKey getKeychain(String username, String name){
    	Directory dir = db.getDirectory(username);
    	DirectoryEntry de = db.getDirectoryEntry(dir.getDirectoryId(), name);
    	if (de == null) return null;
    	Keychain k = db.getKeychain(de.getDirectoryEntryId());
    	if (k != null) return new DirectoryAndKey(de, k);
    	else return null;
    }
    
    public List<DirectoryAndKey> getKeychains(String username) {
    	//Assuming authentication
    	
        Directory dir = db.getDirectory(username);
        if (dir == null) return null;
        
        List<DirectoryAndKey> list = new ArrayList<>();
        List<DirectoryEntry> des = db.getDirectoryEntries(dir.getDirectoryId());
        for (DirectoryEntry de: des){
        	Keychain k = db.getKeychain(de.getDirectoryEntryId());
        	if (k != null){
        		list.add(new DirectoryAndKey(de, k));
        	}
        }
        return list;
    }
}
