package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Optional;

import static postit.shared.Crypto.deserialiseObject;
import static postit.shared.Crypto.serialiseObject;

/**
 * Created by nishadmathur on 8/3/17.
 */
public class Account {
    String username;
    SecretKey secretKey;
    KeyPair encryptionKeypair;
    KeyPair signingKeypair;

    public Account(String username) {
        this.username = username;
        this.secretKey = Crypto.secretKeyFromBytes("DEAD BEEF".getBytes());
        this.encryptionKeypair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
        this.signingKeypair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
    }

    public Account(String username, String password) {
        this.username = username;
        this.secretKey = Crypto.secretKeyFromBytes(password.getBytes());
        this.encryptionKeypair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
        this.signingKeypair = Crypto.generateRSAKeyPair().orElseThrow(() -> new RuntimeException("Missing bouncy castle library!"));
    }

    public Account(JsonObject object) {
        this.username = object.getString("username");
        this.secretKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString("password")));
        this.encryptionKeypair = deserialiseObject(object.getString("encryptionKeypair"));
        this.encryptionKeypair = deserialiseObject(object.getString("signingKeypair"));

    }

    public String getUsername() {
        return username;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public KeyPair getEncryptionKeypair() {
        return encryptionKeypair;
    }

    public KeyPair getSigningKeypair() {
        return signingKeypair;
    }

    public void setEncryptionKeypair(KeyPair encryptionKeypair) {
        this.encryptionKeypair = encryptionKeypair;
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add("username", username)
                .add("password", new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(secretKey))))
                .add("encryptionKeypair", serialiseObject(encryptionKeypair))
                .add("signingKeypair", serialiseObject(signingKeypair));
    }

    public Optional<JsonObjectBuilder> dumpKeypairs() {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] nonce = Crypto.getNonce();

        JsonObjectBuilder dataObject = Json.createObjectBuilder()
                .add("encryptionKeypair", serialiseObject(encryptionKeypair))
                .add("signingKeypair", serialiseObject(signingKeypair));

        Optional<byte[]> data = Crypto.encryptJsonObject(secretKey, nonce, dataObject.build());

        if (!data.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(Json.createObjectBuilder()
                .add("nonce", encoder.encodeToString(nonce))
                .add("data", encoder.encodeToString(data.get())));
    }

    public boolean deserialiseKeypairs(JsonObject jsonObject) {
        Base64.Decoder decoder = Base64.getDecoder();

        try {
            byte[] nonce = decoder.decode(jsonObject.getString("nonce"));
            byte[] data = decoder.decode(jsonObject.getString("data"));

            Optional<JsonObject> decryptedObject = Crypto.decryptJsonObject(secretKey, nonce, data);

            if (!decryptedObject.isPresent()) {
                return false;
            }

            KeyPair encryptionKeypair = deserialiseObject(decryptedObject.get().getString("encryptionKeypair"));
            KeyPair signingKeypair = deserialiseObject(decryptedObject.get().getString("signingKeypair"));

            if (encryptionKeypair != null && signingKeypair != null) {
                this.encryptionKeypair = encryptionKeypair;
                this.signingKeypair = signingKeypair;
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
