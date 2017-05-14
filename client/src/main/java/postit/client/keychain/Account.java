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
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ENCRYPTION_KEYPAIR = "encryptionKeypair";
    public static final String SIGNING_KEYPAIR = "signingKeypair";
    public static final String NONCE = "nonce";
    public static final String DATA = "data";

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
        this.username = object.getString(USERNAME);
        this.secretKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString(PASSWORD)));
        this.encryptionKeypair = deserialiseObject(object.getString(ENCRYPTION_KEYPAIR));
        this.signingKeypair = deserialiseObject(object.getString(SIGNING_KEYPAIR));
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
                .add(USERNAME, username)
                .add(PASSWORD, new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(secretKey))))
                .add(ENCRYPTION_KEYPAIR, serialiseObject(encryptionKeypair))
                .add(SIGNING_KEYPAIR, serialiseObject(signingKeypair));
    }

    public Optional<JsonObjectBuilder> dumpKeypairs(SecretKey keychainEncryptionKey) {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] nonce = Crypto.getNonce();

        JsonObjectBuilder dataObject = Json.createObjectBuilder()
                .add(ENCRYPTION_KEYPAIR, encoder.encodeToString(serialiseObject(encryptionKeypair).getBytes()))
                .add(SIGNING_KEYPAIR, encoder.encodeToString(serialiseObject(signingKeypair).getBytes()));

        Optional<byte[]> data = Crypto.encryptJsonObject(keychainEncryptionKey, nonce, dataObject.build());

        if (!data.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(Json.createObjectBuilder()
                .add(NONCE, encoder.encodeToString(nonce))
                .add(DATA, encoder.encodeToString(data.get())));
    }

    public boolean deserialiseKeypairs(SecretKey keychainEncryptionKey, JsonObject jsonObject) {
        Base64.Decoder decoder = Base64.getDecoder();

        try {
            byte[] nonce = decoder.decode(jsonObject.getString(NONCE));
            byte[] data = decoder.decode(jsonObject.getString(DATA));

            Optional<JsonObject> decryptedObject = Crypto.decryptJsonObject(keychainEncryptionKey, nonce, data);

            if (!decryptedObject.isPresent()) {
                return false;
            }

            KeyPair encryptionKeypair = deserialiseObject(new String(decoder.decode(decryptedObject.get().getString(ENCRYPTION_KEYPAIR))));
            KeyPair signingKeypair = deserialiseObject(new String(decoder.decode(decryptedObject.get().getString(SIGNING_KEYPAIR))));

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
