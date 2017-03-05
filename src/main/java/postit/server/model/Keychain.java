package postit.server.model;

public class Keychain {
	int directoryEntryId;
	String password;
	String metadata;
	
	public Keychain(int directoryEntryId, String password, String metadata){
		this.directoryEntryId = directoryEntryId;
		this.password = password;
		this.metadata = metadata;
	}
	
	public String getPassword(){
		return password;
	}
	
	public String getMetadata(){
		return metadata;
	}
	
	public int getDirectoryEntryId(){
		return directoryEntryId;
	}
	
	public void setPassword(String pwd){
		password = pwd;
	}
	
	public void setMetadata(String metadata){
		this.metadata = metadata;
	}
}
