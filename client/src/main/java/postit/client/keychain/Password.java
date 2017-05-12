package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Password {
    public static final String UUID = "uuid";
    public static final String PASSWORD = "password";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";

    public String uuid;
    public SecretKey password;
    public Map<String, String> metadata;
    public LocalDateTime lastModified;

    public Keychain keychain;

    public Password(String identifier, SecretKey password, Keychain keychain) {
        this.uuid = identifier;
        this.password = password;
        this.metadata = new HashMap<>();
        this.keychain = keychain;
        this.lastModified = LocalDateTime.now();
    }

    public Password(JsonObject object, Keychain keychain) {
        this.keychain = keychain;

        this.uuid = object.getString(UUID);
        this.password = Crypto.secretKeyFromBytes(object.getString(PASSWORD).getBytes());
        this.lastModified = LocalDateTime.parse(object.getString(LAST_MODIFIED));
        this.metadata = new HashMap<>();

        JsonObject metadataObject = object.getJsonObject(METADATA);
        for (String key: metadataObject.keySet()) {
            this.metadata.put(key, metadataObject.getString(key));
        }
    }

    public void markUpdated() {
        this.lastModified = LocalDateTime.now();
        this.keychain.markUpdated();
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder metadataObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataObject.add(entry.getKey(), entry.getValue());
        }

        return Json.createObjectBuilder()
                .add(UUID, uuid)
                .add(PASSWORD, new String(Crypto.secretKeyToBytes(password)))
                .add(LAST_MODIFIED, lastModified.toString())
                .add(METADATA, metadataObject);
    }

    public String getPasswordAsString() {
        return new String(Crypto.secretKeyToBytes(password));
    }

    public void setStringAsPassword(String password) {
        this.password = Crypto.secretKeyFromBytes(password.getBytes());
    }

    public String getTitle() {
        return metadata.get(TITLE);
    }

    public void delete() {
        this.keychain.delete(this);
    }

    @Override
    public String toString() {
        return "Password{" +
                "uuid='" + uuid + '\'' +
                ", password=" + password +
                ", metadata=" + metadata +
                ", lastModified=" + lastModified +
                ", keychain=" + keychain +
                '}';
    }
}
