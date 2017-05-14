package postit.client.keychain;

import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.security.Key;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 27/3/17.
 */
public class DirectoryKeychain {
    private final static Logger LOGGER = Logger.getLogger(DirectoryKeychain.class.getName());
    public static final String PARAMETERS = "parameters";
    public static final String NONCE = "nonce";
    public static final String DATA = "data";
    public static final String KEYCHAIN = "keychain";
    public static final String DIRECTORY = "directory";

    public final JsonObject keychain;
    public final JsonObject entry;

    public final long serverid;

    public DirectoryKeychain(long serverid, JsonObject keychain, JsonObject entry) {
        this.keychain = keychain;
        this.entry = entry;
        this.serverid = serverid;
    }

    public static Optional<DirectoryKeychain> init(long serverid, JsonObject object, Account account, Optional<PublicKey> signatureKey) {
        Base64.Decoder decoder = Base64.getDecoder();

        try {
            JsonObject parameters = object.getJsonObject(PARAMETERS);
            byte[] nonce = decoder.decode(parameters.getString(NONCE));
            byte[] data = decoder.decode(object.getString(DATA));

            String encryptedEncryptionKey = parameters.getString(account.getUsername() + "-key", null);
            String signatureEncryptionKey = parameters.getString(account.getUsername() + "-signature", null);

            if (encryptedEncryptionKey == null || signatureEncryptionKey == null) {
                LOGGER.info("Keychain doesnt have a decryption key/signature for me; Skipping. Perhaps it is too new?");
                return Optional.empty();
            }

            Optional<Key> key = Crypto.unwrapKey(
                    decoder.decode(encryptedEncryptionKey),
                    account.getEncryptionKeypair().getPrivate()
            );

            if (signatureKey.isPresent()) {
                try {
                    if (!Crypto.verify(
                            decoder.decode(encryptedEncryptionKey),
                            decoder.decode(signatureEncryptionKey),
                            signatureKey.get()
                    )) {
                        LOGGER.severe("FAILED TO DECRYPT KEY, TAMPERING OR CORRUPTION DETECTED.");
                        return Optional.empty();
                    }
                } catch (Exception e) {
                    LOGGER.severe("FAILED TO DECRYPT KEY, TAMPERING OR CORRUPTION DETECTED: " + e.getLocalizedMessage());
                    return Optional.empty();
                }
            }

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
                    directorKeychainObject.get().getJsonObject(KEYCHAIN),
                    directorKeychainObject.get().getJsonObject(DIRECTORY)
            ));
        } catch (JsonException | IllegalStateException e) {
            LOGGER.warning("Failed to parse json object: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonObject> dump(DirectoryEntry entryObject, Account account) {
        SecretKey key = Crypto.generateKey();
        byte[] nonce = Crypto.getNonce();

        JsonObjectBuilder directoryKeychain = Json.createObjectBuilder()
                .add(DIRECTORY, entry)
                .add(KEYCHAIN, keychain);

        Base64.Encoder encoder = Base64.getEncoder();

        JsonObjectBuilder encryptedParameters = Json.createObjectBuilder()
                .add(NONCE, encoder.encodeToString(nonce));

        try {
            for (Share share : entryObject.shares) {
                Optional<byte[]> encryptedKey = Crypto.wrapKey(key, share.encryptionKey);

                if (!encryptedKey.isPresent()) {
                    LOGGER.warning("Failed to encrypt dk due to failure to wrap key");
                    return Optional.empty();
                }

                encryptedParameters
                        .add(share.username + "-key", encoder.encodeToString(encryptedKey.get()));
                encryptedParameters
                        .add(share.username + "-signature", encoder.encodeToString(Crypto.signer(encryptedKey.get(), account.signingKeypair.getPrivate())));
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to sign entry: " + e.getMessage());
            return Optional.empty();
        }

        Optional<byte[]> directoryKeychainData = Crypto.encryptJsonObject(key, nonce, directoryKeychain.build());

        if (!directoryKeychainData.isPresent()) {
            LOGGER.warning("Failed to encrypt dk due to failure to encrypt data");
            return Optional.empty();
        }


        return Optional.of(
                Json.createObjectBuilder()
                        .add(DATA, encoder.encodeToString(directoryKeychainData.get()))
                        .add(PARAMETERS, encryptedParameters)
                        .build()
        );
    }

    public long getServerId() {
        return serverid;
    }
}
