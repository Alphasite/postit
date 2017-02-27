package keychain;

import backend.Crypto;

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
    SecretKey password;
    Map<String, String> metadata;

    public Password(JsonObject object) {
        password = Crypto.secretKeyFromBytes(object.getString("password").getBytes());
        metadata = new HashMap<>();

        JsonObject metadataObject = object.getJsonObject("metadata");
        for (String key: metadataObject.keySet()) {
            this.metadata.put(key, metadataObject.getString(key));
        }
    }

    public JsonObjectBuilder dump() {

        JsonObjectBuilder metadataObject = Json.createObjectBuilder();
        metadata.entrySet().stream().map(entry -> metadataObject.add(entry.getKey(), entry.getValue()));

        return Json.createObjectBuilder()
                .add("password", new String(Crypto.secretKeyToBytes(password)))
                .add("metadata", metadataObject);
    }
}
