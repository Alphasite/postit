package postit.account;

/**
 * Created by Zhan on 2/28/2017.
 */
public class Customer {
    String login;
    String firstName;
    String lastName;
    String middleName;
    String email;

    // CONSTRUCTORS
    public Customer(){}

    public Customer(String login, String firstName, String lastName,
                    String middleName, String email) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.email = email;
    }

    // GETTERS
    public String getLogin() {
        return login;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getEmail() {
        return email;
    }

    // SETTERS
    public void setLogin(String login) {
        this.login = login;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
