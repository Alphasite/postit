package keychain;

import javax.json.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class Keychain {
    public String name;
    public List<Password> passwords;
    private DirectoryEntry directoryEntry;

    public Keychain(String name, DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;

        this.name = name;
        this.passwords = new ArrayList<>();
    }

    public Keychain(JsonObject object, DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;

        this.name = object.getString("name");
        this.passwords = new ArrayList<>();

        JsonArray passwordArray = object.getJsonArray("passwords");
        for (int i = 0; i < passwordArray.size(); i++) {
            passwords.add(new Password(passwordArray.getJsonObject(i), this));
        }
    }

    public JsonObjectBuilder dump() {

        JsonArrayBuilder passwordArray = Json.createArrayBuilder();
        for (Password password : passwords) {
            passwordArray.add(password.dump());
        }

        return Json.createObjectBuilder()
                .add("name", name)
                .add("passwords", passwordArray);
    }

    public boolean save() {
        return this.directoryEntry.save();
    }
}
