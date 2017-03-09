package postit.client.keychain;

import postit.client.backend.KeyService;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Base64;

/**
 * Created by nishadmathur on 8/3/17.
 */
public class Account {
    String username;
    SecretKey secretKey;

    Directory directory;

    public Account(String username, Directory directory) {
        this.username = username;
        this.directory = directory;
        this.secretKey = Crypto.secretKeyFromBytes("TODO".getBytes()); // TODO
    }

    public Account(JsonObject object, Directory directory) {
        this.username = object.getString("username");
        this.secretKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString("password")));
        this.directory = directory;
    }

    public String getUsername() {
        return username;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add("username", username)
                .add("password", new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(secretKey))));
    }

    public boolean save() {
        return this.directory.save();
    }
}
