package postit.client.keychain;

import javax.json.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class Keychain {
    public List<Password> passwords;
    public Set<String> deletedPasswords;
    private DirectoryEntry directoryEntry;

    public Keychain(DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
        this.passwords = new ArrayList<>();
        this.deletedPasswords = new HashSet<>();
    }

    public Keychain(JsonObject object, DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
        this.initFrom(object);
    }

    public void initFrom(List<Password> passwords) {
        this.passwords = passwords.stream()
                .filter(password -> !deletedPasswords.contains(password.uuid))
                .collect(Collectors.toList());
    }

    public void initFrom(JsonObject object) {
        this.passwords = new ArrayList<>();
        JsonArray passwordArray = object.getJsonArray("passwords");
        for (int i = 0; i < passwordArray.size(); i++) {
            this.passwords.add(new Password(passwordArray.getJsonObject(i), this));
        }

        this.deletedPasswords = new HashSet<>();
        JsonArray deletedPasswordArray = object.getJsonArray("deleted-passwords");
        for (int i = 0; i < deletedPasswordArray.size(); i++) {
            this.deletedPasswords.add(deletedPasswordArray.getString(i));
        }
    }

    public void markUpdated() {
        this.directoryEntry.markUpdated();
    }

    public JsonObjectBuilder dump() {

        JsonArrayBuilder passwordArray = Json.createArrayBuilder();
        for (Password password : passwords) {
            passwordArray.add(password.dump());
        }

        JsonArrayBuilder deletedPasswordArray = Json.createArrayBuilder();
        for (String deletedPassword : deletedPasswords) {
            deletedPasswordArray.add(deletedPassword);
        }

        return Json.createObjectBuilder()
                .add("passwords", passwordArray)
                .add("deleted-passwords", deletedPasswordArray);
    }

    public String getName() {
        return directoryEntry.name;
    }

    public boolean delete() {
        return this.directoryEntry.delete();
    }

    public void delete(Password password) {
        this.passwords.remove(password);
        this.deletedPasswords.add(password.uuid);
        this.markUpdated();
    }

    public long getServerId(){
    	System.out.println("keyid " + directoryEntry.getServerid());
    	return directoryEntry.getServerid();
    }
}
