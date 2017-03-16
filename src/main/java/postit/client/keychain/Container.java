package postit.client.keychain;

import postit.client.backend.BackingStore;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.*;

/**
 * Created by nishadmathur on 16/3/17.
 */
public class Container {
    public String  salt;

    public String directory;
    public String directoryNonce;

    public Map<String, String> keychains;

    private BackingStore backingStore;

    public Container(BackingStore backingStore) {
        this.backingStore = backingStore;
        this.keychains = new HashMap<>();
    }

    public Container(BackingStore backingStore, JsonObject object) {
        this.backingStore = backingStore;

        this.salt = object.getJsonObject("account").getString("salt");

        this.directory = object.getJsonObject("directory").getString("data");
        this.directoryNonce = object.getJsonObject("directory").getString("nonce");

        this.keychains = new HashMap<>();

        JsonObject keychainsObject = object.getJsonObject("keychains");
        for (String id : keychainsObject.keySet()) {
            this.keychains.put(id, keychainsObject.getString(id));
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

        return Json.createObjectBuilder()
                .add("account", accountObject)
                .add("directory", directoryObject)
                .add("keychains", keychainsObject);
    }
}
