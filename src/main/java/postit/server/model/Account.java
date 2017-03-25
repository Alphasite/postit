package postit.server.model;
import javax.json.*;
/**
 * Created by Zhan on 2/28/2017.
 */
public class Account {
    //Long id; 
    String username;
    String password;
    String email;
    String firstName;
    String lastName;
    String salt;
    
    Customer customer;
    Log log;
    Metadata metadata;

    // CONSTRUCTOR
    public Account(String username, String password, String email, String firstName, String lastName) {
    	this.username = username;
    	this.password = password;
    	this.email = email;
    	this.firstName = firstName;
    	this.lastName = lastName;
    }

    public Account(Account a){
    	this.username = a.username;
    	this.password = a.password;
    	this.email = a.email;
    	this.firstName = a.firstName;
    	this.lastName = a.lastName;
    	this.salt = a.salt;
    }
    
    // GETTERS

//    public Long getId() {
//        return id;
//    }

    public Account() {
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
    
    public Customer getCustomer() {
        return customer;
    }

    public Log getLog() {
        return log;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    // SETTERS
    public void setUsername(String username){
    	this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email){
    	this.email = email;
    }

    public void setFirstname(String firstname){
    	this.firstName = firstname;
    }
    
    public void setLastname(String lastname){
    	this.lastName = lastname;
    }
    
    public void setSalt(String salt){
    	this.salt = salt;
    }
    
    public void setLog(Log log) {
        this.log = log;
    }
    
    public static Account fromJSONObject(JsonObject act){
    	return new Account(act.getString("username"), act.getString("password"), 
    			act.getString("email"), act.getString("firstname"), act.getString("lastname"));
    }
}
