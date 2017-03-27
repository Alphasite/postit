package postit.server.model;

import javax.json.JsonObject;

public class ServerKeychain {
	long directoryEntryId;
	String ownerUsername;
	String name;
	String data;

	public ServerKeychain() {
		this.directoryEntryId = -1;
		this.ownerUsername = null;
		this.name = null;
		this.data = null;
	}

	public ServerKeychain(long id, String ownerUsername, String name, String data){
		directoryEntryId = id;
		this.ownerUsername = ownerUsername;
		this.name = name;
		this.data = data;
	}

	public ServerKeychain(JsonObject object) {
		this.directoryEntryId = object.getInt("directoryEntryId");
		this.ownerUsername = object.getString("ownerUsername");
		this.name = object.getString("name");
		this.data = object.getString("data");
	}

	public long getDirectoryEntryId(){
		return directoryEntryId;
	}
	
	public String getName(){
		return name;
	}
	
	public void setDirectoryEntryId(long directoryEntryId){
		this.directoryEntryId = directoryEntryId;
	}
	
	public void setName(String name){
		this.name = name;
	}

	public String getOwnerUsername() {
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
