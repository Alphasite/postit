package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;
import postit.client.backend.KeyService;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(DirectoryEntry.class.getName());

    public String name;
    public long serverid;

    private SecretKey encryptionKey;
    private byte[] nonce;

    Directory directory;
    Keychain keychain;

    BackingStore backingStore;

    public LocalDateTime lastModified;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, BackingStore backingStore) {
        this.name = name;
        this.setEncryptionKey(encryptionKey);
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.lastModified = LocalDateTime.now();
        this.serverid = -1L;
    }

    public DirectoryEntry(JsonObject object, Directory directory, BackingStore backingStore) {
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.updateFrom(object);
    }

    public void updateFrom(JsonObject object) {
        Base64.Decoder decoder = Base64.getDecoder();
        this.name = object.getString("name");
        this.setEncryptionKey(Crypto.secretKeyFromBytes(decoder.decode(object.getString("encryption-key"))));
        this.setNonce(decoder.decode(object.getString("nonce")));
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.lastModified = LocalDateTime.parse(object.getString("lastModified"));
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Base64.Encoder encoder = Base64.getEncoder();

        builder.add("name", name);
        builder.add("encryption-key", encoder.encodeToString(Crypto.secretKeyToBytes(getEncryptionKey())));
        builder.add("nonce", encoder.encodeToString(getNonce()));
        builder.add("serverid", serverid);
        builder.add("lastModified", lastModified.toString()); // TODO check this handles timezones correctly

        return builder;
    }

    public Optional<Keychain> readKeychain() {
        LOGGER.info("Reading keychain " + name);

        // If it has already been loaded, return that.
        if (this.keychain != null) {
            return Optional.of(keychain);
        }

        Optional<Keychain> keychain = this.backingStore.readKeychain(this);

        if (keychain.isPresent()) {
            LOGGER.info("Succeeded in reading keychain " + name + " from disk.");
            this.keychain = keychain.get();
            return keychain;
        } else {
            return Optional.empty();
        }
    }

    public boolean delete() {
        return this.directory.delete(this);
    }

    public SecretKey getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(SecretKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }
}
