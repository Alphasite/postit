package postit.server.model;

public class Directory {
	String username;
	int directoryId;
	String ownPath;
	
	public Directory(int directoryId, String username, String ownPath){
		this.username = username;
		this.directoryId = directoryId;
		this.ownPath = ownPath;
	}
	
	public int getDirectoryId(){
		return directoryId;
	}
}
