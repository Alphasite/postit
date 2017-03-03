package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Password {
    public String identifier;
    public SecretKey password;
    public Map<String, String> metadata;

    private Keychain keychain;

    public Password(String identifier, SecretKey password, Keychain keychain) {
        this.identifier = identifier;
        this.password = password;
        this.metadata = new HashMap<>();
        this.keychain = keychain;
    }

    public Password(JsonObject object, Keychain keychain) {
        this.keychain = keychain;

        this.identifier = object.getString("identifier");
        this.password = Crypto.secretKeyFromBytes(object.getString("password").getBytes());
        this.metadata = new HashMap<>();

        JsonObject metadataObject = object.getJsonObject("metadata");
        for (String key: metadataObject.keySet()) {
            this.metadata.put(key, metadataObject.getString(key));
        }
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder metadataObject = Json.createObjectBuilder();
        metadata.entrySet().stream().map(entry -> metadataObject.add(entry.getKey(), entry.getValue()));

        return Json.createObjectBuilder()
                .add("identifier", identifier)
                .add("password", new String(Crypto.secretKeyToBytes(password)))
                .add("metadata", metadataObject);
    }

    public boolean save() {
        return this.keychain.save();
    }

    public boolean delete() {
        this.keychain.passwords.remove(this);
        if (this.keychain.save()) {
            return true;
        } else {
            this.keychain.passwords.add(this);
            return false;
        }
    }
}
