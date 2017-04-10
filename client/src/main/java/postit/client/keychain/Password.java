package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
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

    public Keychain keychain;

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

    public void markUpdated() {
        this.keychain.markUpdated();
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder metadataObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataObject.add(entry.getKey(), entry.getValue());
        }

        return Json.createObjectBuilder()
                .add("identifier", identifier)
                .add("password", new String(Crypto.secretKeyToBytes(password)))
                .add("metadata", metadataObject);
    }

    public String getPasswordAsString() {
        return new String(Crypto.secretKeyToBytes(password));
    }

    public void setStringAsPassword(String password) {
        this.password = Crypto.secretKeyFromBytes(password.getBytes());
    }

    public boolean delete() {
        this.keychain.passwords.remove(this);
        return true;
    }

    @Override
    public String toString() {
        return "Password{" +
                "identifier='" + identifier + '\'' +
                ", password=" + getPasswordAsString() +
                '}';
    }
}
