package keychain;

import javax.json.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class Keychain {
    String name;
    List<Password> passwords;

    public Keychain(String name) {
        this.name = name;
    }

    public Keychain(JsonObject object) {
        name = object.getString("name");
        passwords = new ArrayList<>();

        JsonArray passwordArray = object.getJsonArray("passwords");
        for (int i = 0; i < passwordArray.size(); i++) {
            passwords.add(new Password(passwordArray.getJsonObject(i)));
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
}
