package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.StringReader;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 27/3/17.
 */
public class DirectoryKeychain {
    private final static Logger LOGGER = Logger.getLogger(DirectoryKeychain.class.getName());

    public final JsonObject keychain;
    public final JsonObject entry;

    public DirectoryKeychain(JsonObject keychain, JsonObject entry) {
        this.keychain = keychain;
        this.entry = entry;
    }

    public static Optional<DirectoryKeychain> init(JsonObject object, PrivateKey privateKey) {
        Base64.Decoder decoder = Base64.getDecoder();

        try {
            JsonObject parameters = object.getJsonObject("parameters");
            byte[] nonce = decoder.decode(parameters.getString("nonce"));
            byte[] data = decoder.decode(object.getString("data"));
            Optional<Key> key = Crypto.unwrapKey(decoder.decode(parameters.getString("key")), privateKey);
            
            if (!key.isPresent()) {
                LOGGER.severe("Could not decrypt parameters due to error unwrapping key.");
                return Optional.empty();
            }

            Optional<JsonObject> directorKeychainObject = Crypto.decryptJsonObject(key.get(), nonce, data);

            if (!directorKeychainObject.isPresent()) {
                LOGGER.warning("Failed to decrypt dk object.");
                return Optional.empty();
            }


            return Optional.of(new DirectoryKeychain(
                    Json.createReader(new StringReader(directorKeychainObject.get().getString("keychain"))).readObject(),
                    Json.createReader(new StringReader(directorKeychainObject.get().getString("directory"))).readObject()
            ));
        } catch (JsonException | IllegalStateException e) {
            LOGGER.warning("Failed to parse json object: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonObject> dump(PublicKey publicKey) {
        SecretKey key = Crypto.generateKey();
        byte[] nonce = Crypto.getNonce();

        JsonObjectBuilder directoryKeychain = Json.createObjectBuilder()
                .add("directory", entry)
                .add("keychain", keychain);

        Optional<byte[]> directoryKeychainData = Crypto.encryptJsonObject(key, nonce, directoryKeychain.build());

        if (!directoryKeychainData.isPresent()) {
            LOGGER.warning("Failed to encrypt dk due to failure to encrypt data");
            return Optional.empty();
        }

        Base64.Encoder encoder = Base64.getEncoder();

        Optional<byte[]> encryptedKey = Crypto.wrapKey(key, publicKey);

        if (!encryptedKey.isPresent()) {
            LOGGER.warning("Failed to encrypt dk due to failure to wrap key");
            return Optional.empty();
        }

        JsonObjectBuilder encryptedParameters = Json.createObjectBuilder()
                .add("key", encoder.encodeToString(encryptedKey.get()))
                .add("nonce", encoder.encodeToString(nonce));

        return Optional.of(
                Json.createObjectBuilder()
                        .add("data", encoder.encodeToString(directoryKeychainData.get()))
                        .add("parameters", encryptedParameters)
                        .build()
        );
    }
}
