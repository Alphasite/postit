package postit.shared.model;

import org.json.JSONObject;

import postit.server.model.DirectoryEntry;
import postit.server.model.Keychain;

/**
 * A wrapper class containing information of both DirectoryEntry and Keychain
 * @author Ning
 *
 */
public class DirectoryAndKey {
	int directoryEntryId;
	String name;
	String encryptionKey;
	int directoryId;
	String password;
	String metadata;
	
	public DirectoryAndKey(){
		directoryEntryId = -1;
		directoryId = -1;
	}
	
	public DirectoryAndKey(int id, String name, String key, int dirId, String password, String metadata){
		this.directoryEntryId = id;
		this.name = name;
		encryptionKey = key;
		directoryId = dirId;
		this.password = password;
		this.metadata = metadata;
	}
	
	public DirectoryAndKey(DirectoryEntry de, Keychain k){
		assert de.getDirectoryEntryId() == k.getDirectoryEntryId();
		
		directoryEntryId = de.getDirectoryEntryId();
		name = de.getName();
		encryptionKey = de.getEncryptionKey();
		directoryId = de.getDirectoryId();
		password = k.getPassword();
		metadata = k.getMetadata();
	}
	
	public int getDirectoryEntryId(){
		return directoryEntryId;
	}
	
	public int getDirectoryId(){
		return directoryId;
	}
	
	public String getName(){
		return name;
	}
	
	public String getEncryptionKey(){
		return encryptionKey;
	}
	
	public void setDirectoryEntryId(int directoryEntryId){
		this.directoryEntryId = directoryEntryId;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setEncryptionKey(String key){
		encryptionKey = key;
	}
	
	public String getPassword(){
		return password;
	}
	
	public String getMetadata(){
		return metadata;
	}
	
	public void setPassword(String pwd){
		password = pwd;
	}
	
	public void setMetadata(String metadata){
		this.metadata = metadata;
	}
	
	public void setDirectoryId(int id){
		directoryId = id;
	}
	
	public void convertToDirectoryAndKey(DirectoryEntry de, Keychain k){
		if (de == null || k == null) return;
		de.setDirectoryEntryId(directoryEntryId);
		de.setName(name);
		de.setEncryptionKey(encryptionKey);
		de.setDirectoryId(directoryId);
		k.setDirectoryEntryId(directoryEntryId);
		k.setMetadata(metadata);
		k.setPassword(password);
	}
	
	public static DirectoryAndKey fromJSONObject(JSONObject obj){
		return new DirectoryAndKey(obj.getInt("directoryEntryId"), obj.getString("name"), obj.getString("encryptionKey"), 
				obj.getInt("directoryId"), obj.getString("password"), obj.getString("metadata"));
	}
}
