package postit.server.model;

public class DirectoryEntry {
	int directoryEntryId;
	String name;
	String encryptionKey;
	int directoryId;
	
	public DirectoryEntry(int id, String name, String key, int dirId){
		directoryEntryId = id;
		this.name = name;
		encryptionKey = key;
		directoryId = dirId;
	}
	
	public int getDirectoryEntryId(){
		return directoryEntryId;
	}
	
	public String getName(){
		return name;
	}
	
	public String getEncryptionKey(){
		return encryptionKey;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setEncryptionKey(String key){
		encryptionKey = key;
	}
}
