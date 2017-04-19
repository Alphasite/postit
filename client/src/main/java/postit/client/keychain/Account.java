package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.KeyPair;
import java.util.Base64;

import static postit.shared.Crypto.deserialiseObject;
import static postit.shared.Crypto.serialiseObject;

/**
 * Created by nishadmathur on 8/3/17.
 */
public class Account {
    String username;
    SecretKey secretKey;
    KeyPair keyPair;

    public Account(String username) {
        this.username = username;
        this.secretKey = Crypto.secretKeyFromBytes("DEAD BEEF".getBytes());
        this.keyPair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
    }

    public Account(String username, String password) {
        this.username = username;
        this.secretKey = Crypto.secretKeyFromBytes(password.getBytes());
        this.keyPair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
    }

    public Account(JsonObject object) {
        this.username = object.getString("username");
        this.secretKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString("password")));
        this.keyPair = deserialiseObject(object.getString("keypair"));

    }

    public String getUsername() {
        return username;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add("username", username)
                .add("password", new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(secretKey))))
                .add("keypair", serialiseObject(keyPair));
    }
}
