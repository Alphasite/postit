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

        this.salt = object.getJsonObject("account").getString("salt");

        this.directory = object.getJsonObject("directory").getString("data");
        this.directoryNonce = object.getJsonObject("directory").getString("nonce");

        this.keychains = new HashMap<>();
        this.blobs = new HashMap<>();

        JsonObject keychainsObject = object.getJsonObject("keychains");
        for (String id : keychainsObject.keySet()) {
            this.keychains.put(id, keychainsObject.getString(id));
        }

        JsonObject blobObject = object.getJsonObject("blobs");
        for (String id : blobObject.keySet()) {
            this.blobs.put(id, blobObject.getString(id));
        }
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder accountObject = Json.createObjectBuilder()
                .add("salt", this.salt);

        JsonObjectBuilder directoryObject = Json.createObjectBuilder()
                .add("data", directory)
                .add("nonce", directoryNonce);

        JsonObjectBuilder keychainsObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> keychainEntry : keychains.entrySet()) {
            keychainsObject.add(keychainEntry.getKey(), keychainEntry.getValue());
        }

        JsonObjectBuilder blobObject = Json.createObjectBuilder();
        for (Map.Entry<String, String> blob : blobs.entrySet()) {
            keychainsObject.add(blob.getKey(), blob.getValue());
        }

        return Json.createObjectBuilder()
                .add("account", accountObject)
                .add("directory", directoryObject)
                .add("keychains", keychainsObject)
                .add("blobs", blobObject);
    }
}
