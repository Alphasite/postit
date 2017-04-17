package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.*;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

    public List<Share> shares;

    BackingStore backingStore;

    public LocalDateTime lastModified;

    public String owner;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, BackingStore backingStore) {
        this.name = name;
        this.setEncryptionKey(encryptionKey);
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.lastModified = LocalDateTime.now();
        this.serverid = -1L;
        this.shares = new ArrayList<>();
        this.owner = null;
    }

    public DirectoryEntry(JsonObject object, Directory directory, BackingStore backingStore) {
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.shares = new ArrayList<>();
        this.updateFrom(object);
    }

    public void updateFrom(JsonObject object) {
        Base64.Decoder decoder = Base64.getDecoder();
        this.name = object.getString("name");
        this.setEncryptionKey(Crypto.secretKeyFromBytes(decoder.decode(object.getString("encryption-key"))));
        this.setNonce(decoder.decode(object.getString("nonce")));
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.lastModified = LocalDateTime.parse(object.getString("lastModified"));
        this.owner = object.getString("owner", null);

        JsonArray shareArray = object.getJsonArray("passwords");
        for (int i = 0; i < shareArray.size(); i++) {
            try {
                this.shares.add(new Share(shareArray.getJsonObject(i)));
            } catch (InvalidKeyException e) {
                LOGGER.warning("Failed to parse RSA key: " + e.getMessage() + " ignoring.");
            }
        }
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Base64.Encoder encoder = Base64.getEncoder();

        builder.add("name", name);
        builder.add("encryption-key", encoder.encodeToString(Crypto.secretKeyToBytes(getEncryptionKey())));
        builder.add("nonce", encoder.encodeToString(getNonce()));
        builder.add("serverid", serverid);
        builder.add("lastModified", lastModified.toString()); // TODO check this handles timezones correctly

        if (this.owner == null && this.directory.getAccount().isPresent()) {
            this.owner = this.directory.getAccount().get().getUsername();
        }

        if (this.owner != null) {
            builder.add("owner", this.owner);
        }

        JsonArrayBuilder shareArray = Json.createArrayBuilder();
        for (Share share: shares) {
            shareArray.add(share.dump());
        }

        builder.add("shared", shareArray);

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

    public void markUpdated() {
        this.lastModified = LocalDateTime.now();
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
        return nonce.clone();
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce.clone();
    }

    public void setName(String name){ this.name = name;}
}
