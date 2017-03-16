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

    public SecretKey encryptionKey;
    public byte[] nonce;

    Directory directory;
    Keychain keychain;

    KeyService keyService;
    BackingStore backingStore;

    public LocalDateTime lastModified;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.name = name;
        this.encryptionKey = encryptionKey;
        this.directory = directory;
        this.keychain = null;
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.lastModified = LocalDateTime.now();
        this.serverid = -1L;
    }

    public DirectoryEntry(JsonObject object, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.directory = directory;
        this.keychain = null;
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.updateFrom(object);
    }

    public void updateFrom(JsonObject object) {
        this.name = object.getString("name");
        this.encryptionKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString("encryption-key").getBytes()));
        this.nonce = Base64.getDecoder().decode(object.getString("nonce"));
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.lastModified = LocalDateTime.parse(object.getString("lastModified"));
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("encryption-key", new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(encryptionKey))));
        builder.add("nonce", Base64.getEncoder().encodeToString(nonce));
        builder.add("serverid", serverid);
        builder.add("lastModified", lastModified.toString()); // TODO check this handles timezones correctly

        return builder;
    }

    public Optional<Keychain> readKeychain() {
        LOGGER.info("Reading keychain " + name);

        // If it has already been loaded, return that.
        if (keychain != null) {
            return Optional.of(keychain);
        }

        Optional<String> backingStore = this.backingStore.readKeychain(name);

        // Create the keychain file if it doesnt already exist.
        if (!backingStore.isPresent()) {
            LOGGER.info("Keychain file doesn't exist... creating it: " + name);

            keychain = new Keychain(this);

            return Optional.of(keychain);
        }

        // otherwise load and decrypt it.
        Optional<JsonObject> object = Crypto.decryptJsonObject(
                this.encryptionKey,
                this.nonce,
                Base64.getDecoder().decode(backingStore.get())
        );

        if (object.isPresent()) {
            LOGGER.info("Succeeded in reading keychain " + name + " from disk.");
            keychain = new Keychain(object.get(), this);
            return Optional.of(keychain);
        } else {
            return Optional.empty();
        }
    }

    public boolean delete() {
        return this.directory.delete(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectoryEntry entry = (DirectoryEntry) o;

        if (serverid != entry.serverid) return false;
        if (!name.equals(entry.name)) return false;
        if (!encryptionKey.equals(entry.encryptionKey)) return false;
        if (!Arrays.equals(nonce, entry.nonce)) return false;
        return lastModified.equals(entry.lastModified);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) (serverid ^ (serverid >>> 32));
        result = 31 * result + encryptionKey.hashCode();
        result = 31 * result + Arrays.hashCode(nonce);
        result = 31 * result + lastModified.hashCode();
        return result;
    }
}
