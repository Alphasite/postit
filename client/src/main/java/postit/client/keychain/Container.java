package postit.client.keychain;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nishadmathur on 16/3/17.
 */
public class Container {
    public static final String ACCOUNT = "account";
    public static final String SALT = "salt";
    public static final String DIRECTORY = "directory";
    public static final String DATA = "data";
    public static final String NONCE = "nonce";
    public static final String KEYCHAINS = "keychains";
    public static final String BLOBS = "blobs";
    public String  salt;

    public String directory;
    public String directoryNonce;

    public Map<String, String> keychains;

    public Map<String, String> blobs;

    public Container() {
        this.keychains = new HashMap<>();
        this.blobs = new HashMap<>();
    }

    public Container(JsonObject object) {

        this.salt = object.getJsonObject(ACCOUNT).getString(SALT);

        this.directory = object.getJsonObject(DIRECTORY).getString(DATA);
        this.directoryNonce = object.getJsonObject(DIRECTORY).getString(NONCE);

        this.keychains = new HashMap<>();
        this.blobs = new HashMap<>();

        JsonObject keychainsObject = object.getJsonObject(KEYCHAINS);
        for (String id : keychainsObject.keySet()) {
            this.keychains.put(id, keychainsObject.getString(id));
        }

        JsonObject blobObject = object.getJsonObject(BLOBS);
        for (String id : blobObject.keySet()) {
            this.blobs.put(id, blobObject.getString(id));
        }
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder accountObject = Json.createObjectBuilder()
                .add(SALT, this.salt);

        JsonObjectBuilder directoryObject = Json.createObjectBuilder()
                .add(DATA, directory)
                .add(NONCE, directoryNonce);

        JsonObjectBuilder keychainsObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> keychainEntry : keychains.entrySet()) {
            keychainsObject.add(keychainEntry.getKey(), keychainEntry.getValue());
        }

        JsonObjectBuilder blobObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> blob : blobs.entrySet()) {
            keychainsObject.add(blob.getKey(), blob.getValue());
        }

        return Json.createObjectBuilder()
                .add(ACCOUNT, accountObject)
                .add(DIRECTORY, directoryObject)
                .add(KEYCHAINS, keychainsObject)
                .add(BLOBS, blobObject);
    }
}
