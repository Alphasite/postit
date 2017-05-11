package postit.server.model;

import javax.json.JsonObject;

/**
 * Created by Zhan on 2/28/2017.
 */
public class ServerAccount {
    //Long id; 
    String username;
    String password;
    String email;
    String firstName;
    String lastName;
    String salt;
    String phoneNumber;
    String keypair;
    String publickey;

    // CONSTRUCTOR
    public ServerAccount(String username, String password, String email, String firstName, String lastName, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public ServerAccount(String username, String password, String email, String firstName, String lastName, String phoneNumber, String keypair, String publickey) {
    	this.username = username;
    	this.password = password;
    	this.email = email;
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.phoneNumber = phoneNumber;
    	this.keypair = keypair;
    	this.publickey = publickey;
    }

    public ServerAccount(ServerAccount a){
    	this.username = a.username;
    	this.password = a.password;
    	this.email = a.email;
    	this.firstName = a.firstName;
    	this.lastName = a.lastName;
    	this.salt = a.salt;
    	this.phoneNumber = a.phoneNumber;
    	this.keypair = a.keypair;
    	this.publickey = a.publickey;
    }
    
    // GETTERS

//    public Long getId() {
//        return id;
//    }

    public ServerAccount() {
	}

	public String getUsername(){
    	return username;
    }
    
    public String getPassword() {
        return password;
    }

    public String getEmail(){
    	return email;
    }
    
    public String getFirstname(){
    	return firstName;
    }
    
    public String getLastname(){
    	return lastName;
    }

    public String getSalt(){
    	return salt;
    }
    
    public String getPhoneNumber(){
    	return phoneNumber;
    }

    public String getKeypair() { return this.keypair; }

    public String getPublickey() { return this.publickey; }

    // SETTERS
    public void setUsername(String username){
    	this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) { this.email = email; }

    public void setFirstname(String firstname){
    	this.firstName = firstname;
    }
    
    public void setLastname(String lastname){
    	this.lastName = lastname;
    }
    
    public void setSalt(String salt){
    	this.salt = salt;
    }

    public void setKeypair(String keypair) {this.keypair = keypair; }

    public void setPublickey(String publickey) {this.publickey = publickey; }

    public void setPhoneNumber(String phoneNumber){
    	this.phoneNumber = phoneNumber;
    }
    public static ServerAccount fromJSONObject(JsonObject act){
        if (act.getString("keypair") != null) {
            return new ServerAccount(
                    act.getString("username"),
                    act.getString("password"),
                    act.getString("email"),
                    act.getString("firstname"),
                    act.getString("lastname"),
                    act.getString("phoneNumber"),
                    act.getString("keypair"),
                    act.getString("publickey")
            );
        }
    	return new ServerAccount(
    	        act.getString("username"),
                act.getString("password"),
    			act.getString("email"),
                act.getString("firstname"),
                act.getString("lastname"),
                act.getString("phoneNumber")
        );
    }
}
