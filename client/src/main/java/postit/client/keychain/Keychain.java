package postit.client.keychain;

import javax.json.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class Keychain {
    public List<Password> passwords;
    private DirectoryEntry directoryEntry;

    public Keychain(DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
        this.passwords = new ArrayList<>();
    }

    public Keychain(JsonObject object, DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
        this.initFrom(object);
    }

    public void initFrom(List<Password> passwords) {
        this.passwords = new ArrayList<>(passwords);
    }

    public void initFrom(JsonObject object) {
        this.passwords = new ArrayList<>();
        JsonArray passwordArray = object.getJsonArray("passwords");
        for (int i = 0; i < passwordArray.size(); i++) {
            this.passwords.add(new Password(passwordArray.getJsonObject(i), this));
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

        return Json.createObjectBuilder()
                .add("passwords", passwordArray);
    }

    public String getName() {
        return directoryEntry.name;
    }

    public boolean delete() {
        return this.directoryEntry.delete();
    }
}