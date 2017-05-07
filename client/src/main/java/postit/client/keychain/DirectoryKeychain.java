package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.Key;
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

    public final long serverid;

    public DirectoryKeychain(long serverid, JsonObject keychain, JsonObject entry) {
        this.keychain = keychain;
        this.entry = entry;
        this.serverid = serverid;
    }

    public static Optional<DirectoryKeychain> init(long serverid, JsonObject object, Account account) {
        Base64.Decoder decoder = Base64.getDecoder();

        try {
            JsonObject parameters = object.getJsonObject("parameters");
            byte[] nonce = decoder.decode(parameters.getString("nonce"));
            byte[] data = decoder.decode(object.getString("data"));

            String encryptedEncryptionKey = parameters.getString(account.getUsername() + "-key", null);

            if (encryptedEncryptionKey == null) {
                LOGGER.info("Keychain doesnt have a decryption key for me; Skipping. Perhaps it is too new?");
                return Optional.empty();
            }

            Optional<Key> key = Crypto.unwrapKey(
                    decoder.decode(encryptedEncryptionKey),
                    account.getEncryptionKeypair().getPrivate()
            );

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
                    serverid,
                    directorKeychainObject.get().getJsonObject("keychain"),
                    directorKeychainObject.get().getJsonObject("directory")
            ));
        } catch (JsonException | IllegalStateException e) {
            LOGGER.warning("Failed to parse json object: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonObject> dump(DirectoryEntry entryObject) {
        SecretKey key = Crypto.generateKey();
        byte[] nonce = Crypto.getNonce();

        JsonObjectBuilder directoryKeychain = Json.createObjectBuilder()
                .add("directory", entry)
                .add("keychain", keychain);

        Base64.Encoder encoder = Base64.getEncoder();

        JsonObjectBuilder encryptedParameters = Json.createObjectBuilder()
                .add("nonce", encoder.encodeToString(nonce));

        for (Share share : entryObject.shares) {
            Optional<byte[]> encryptedKey = Crypto.wrapKey(key, share.publicKey);

            if (!encryptedKey.isPresent()) {
                LOGGER.warning("Failed to encrypt dk due to failure to wrap key");
                return Optional.empty();
            }

            encryptedParameters
                    .add(share.username + "-key", encoder.encodeToString(encryptedKey.get()));
        }

        Optional<byte[]> directoryKeychainData = Crypto.encryptJsonObject(key, nonce, directoryKeychain.build());

        if (!directoryKeychainData.isPresent()) {
            LOGGER.warning("Failed to encrypt dk due to failure to encrypt data");
            return Optional.empty();
        }


        return Optional.of(
                Json.createObjectBuilder()
                        .add("data", encoder.encodeToString(directoryKeychainData.get()))
                        .add("parameters", encryptedParameters)
                        .build()
        );
    }

    public long getServerid() {
        return serverid;
    }
}
