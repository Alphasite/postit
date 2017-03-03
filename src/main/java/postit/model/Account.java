package postit.model;
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
    
    JsonObject keychains;
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

    // GETTERS

//    public Long getId() {
//        return id;
//    }

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
    
    public JsonObject getKeychains() {
        return keychains;
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
    public void setPassword(String password) {
        this.password = password;
    }

    public void setKeychains(JsonObject keychains) {
        this.keychains = keychains;
    }

    public void setLog(Log log) {
        this.log = log;
    }
}
