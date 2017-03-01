package account;
import javax.json.*;
/**
 * Created by Zhan on 2/28/2017.
 */
public class Account {
    Long id;
    String password;
    JsonObject keychains;
    Customer customer;
    Log log;
    Metadata metadata;

    // CONSTRUCTOR
    public Account() {}

    // GETTERS

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
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
