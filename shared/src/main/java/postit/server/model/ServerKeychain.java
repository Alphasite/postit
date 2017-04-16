package postit.server.model;

import javax.json.JsonObject;

public class ServerKeychain {
	private long directoryEntryId;
	private long ownerDirectoryEntryId;
	private String ownerUsername;
	private String sharedUsername;
	private boolean sharedHasWritePermission;
	private String name;
	private String data;

	public ServerKeychain() {
		this.directoryEntryId = -1;
		this.ownerUsername = null;
		this.ownerDirectoryEntryId = -1;
		this.sharedUsername = null;
		this.sharedHasWritePermission = false;
		this.name = null;
		this.data = null;
	}

	public ServerKeychain(long id, String ownerUsername, long ownerDirectoryEntryId, String sharedUsername, boolean sharedHasWritePermission, String name, String data){
		this.directoryEntryId = id;
		this.ownerUsername = ownerUsername;
		this.ownerDirectoryEntryId = ownerDirectoryEntryId;
		this.sharedUsername = sharedUsername;
		this.sharedHasWritePermission = sharedHasWritePermission;
		this.name = name;
		this.data = data;
	}

	public ServerKeychain(JsonObject object) {
		this.directoryEntryId = object.getJsonNumber("directoryEntryId").longValue();
		this.ownerUsername = object.getString("ownerUsername");
		this.ownerDirectoryEntryId = object.getJsonNumber("ownerDirectoryEntryId").longValue();
		this.sharedUsername = object.getString("sharedUsername");
		this.sharedHasWritePermission = object.getBoolean("sharedUserCanWrite");
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

	public String getSharedUsername() {
		return sharedUsername;
	}

	public void setSharedUsername(String sharedUsername) {
		this.sharedUsername = sharedUsername;
	}

	public boolean isSharedHasWritePermission() {
		return sharedHasWritePermission;
	}

	public void setSharedHasWritePermission(boolean sharedHasWritePermission) {
		this.sharedHasWritePermission = sharedHasWritePermission;
	}

	public long getOwnerDirectoryEntryId() {
		return ownerDirectoryEntryId;
	}

	public void setOwnerDirectoryEntryId(long ownerDirectoryEntryId) {
		this.ownerDirectoryEntryId = ownerDirectoryEntryId;
	}
}
