package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.*;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(DirectoryEntry.class.getName());

    public static final String SHARES = "shares";
    public static final String DELETED_SHARES = "deleted-shares";
    public static final String LOG = "log";
    public static final String ENCRYPTION_KEY = "encryption-key";
    public static final String NONCE = "nonce";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String NAME = "name";
    public static final String UUID = "uuid";
    public static final String SERVERID = "serverid";

    public String name;
    private Share share;

    public String uuid;

    private SecretKey encryptionKey;
    private byte[] nonce;

    Directory directory;
    Keychain keychain;

    public List<Share> shares;

    public Set<String> log;

    public Set<String> deletedShares;

    BackingStore backingStore;

    public LocalDateTime lastModified;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, BackingStore backingStore, RSAPublicKey publicKey, RSAPublicKey signingKey) {
        this.name = name;
        this.setEncryptionKey(encryptionKey);
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.lastModified = LocalDateTime.now();
        this.uuid = java.util.UUID.randomUUID().toString();
        this.log = new HashSet<>();
        this.share = new Share(-1, null, true, publicKey, signingKey, true);
        this.shares = new ArrayList<>();
        this.shares.add(this.share);
        this.deletedShares = new HashSet<>();
    }

    public DirectoryEntry(String username, JsonObject object, Directory directory, BackingStore backingStore) {
        this.directory = directory;
        this.keychain = null;
        this.backingStore = backingStore;
        this.shares = new ArrayList<>();
        this.log = new HashSet<>();
        this.deletedShares = new HashSet<>();
        this.updateFrom(object);

        JsonArray shareArray = object.getJsonArray(SHARES);
        for (int i = 0; i < shareArray.size(); i++) {
            try {
                Share share = new Share(shareArray.getJsonObject(i));
                this.shares.add(share);

                if (share.username.equals(username)) {
                    this.share = share;
                }
            } catch (InvalidKeyException e) {
                LOGGER.warning("Failed to parse RSA key: " + e.getMessage() + " ignoring.");
            }
        }

        JsonArray logArray = object.getJsonArray(LOG);
        for (int i = 0; i < logArray.size(); i++) {
            this.log.add(logArray.getString(i));
        }

        JsonArray deletedSharesArray = object.getJsonArray(DELETED_SHARES);
        for (int i = 0; i < deletedSharesArray.size(); i++) {
            this.deletedShares.add(deletedSharesArray.getString(i));
        }

        if (this.deletedShares.contains("-1")) {
            this.deletedShares.remove("-1");
        }
    }

    public void updateFrom(JsonObject object) {
        Base64.Decoder decoder = Base64.getDecoder();
        this.name = object.getString(NAME);
        this.setEncryptionKey(Crypto.secretKeyFromBytes(decoder.decode(object.getString(ENCRYPTION_KEY))));
        this.setNonce(decoder.decode(object.getString(NONCE)));
        this.lastModified = LocalDateTime.parse(object.getString(LAST_MODIFIED));
        this.uuid = object.getString(UUID);
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Base64.Encoder encoder = Base64.getEncoder();

        builder.add(NAME, name);
        builder.add(ENCRYPTION_KEY, encoder.encodeToString(Crypto.secretKeyToBytes(getEncryptionKey())));
        builder.add(NONCE, encoder.encodeToString(getNonce()));
        builder.add(SERVERID, getServerId());
        builder.add(LAST_MODIFIED, lastModified.toString()); // TODO check this handles timezones correctly
        builder.add(UUID, uuid);

        JsonArrayBuilder shareArray = Json.createArrayBuilder();
        for (Share share: shares) {
            shareArray.add(share.dump());
        }

        JsonArrayBuilder logArray = Json.createArrayBuilder();
        for (String entry : log) {
            logArray.add(entry);
        }

        JsonArrayBuilder deletedSharesArray = Json.createArrayBuilder();
        for (String share : deletedShares) {
            logArray.add(share);
        }

        builder.add(SHARES, shareArray);
        builder.add(LOG, logArray);
        builder.add(DELETED_SHARES, deletedSharesArray);

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

    public long getServerId() {
        return this.share.serverid;
    }

    public void setServerid(long serverid) {
        this.share.serverid = serverid;
    }

    public String getOwner() {
        return this.share.username;
    }

    public void setOwner(String owner) {
        this.share.username = owner;
    }

    public void addLogEntry(String entry) {
        this.log.add(entry);
    }

    public Set<String> getLog() {
        return log;
    }
}
