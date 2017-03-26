package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;

import javax.crypto.SecretKey;
import javax.json.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Directory {
    private final static Logger LOGGER = Logger.getLogger(Directory.class.getName());

    private BackingStore backingStore;
    private KeyService keyService;

    public List<DirectoryEntry> keychains;
    public List<Long> deletedKeychains;

    public Account account;

    public Directory(KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();
        this.account = keyService.getAccount();
    }

    public Directory(JsonObject object, KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();

        this.account = new Account(object.getJsonObject("account"));

        JsonArray keychainArray = object.getJsonArray("keychains");
        for (int i = 0; i < keychainArray.size(); i++) {
            keychains.add(new DirectoryEntry(keychainArray.getJsonObject(i), this, backingStore));
        }

        JsonArray deletedKeychainsArray = object.getJsonArray("deleted");
        for (int i = 0; i < deletedKeychainsArray.size(); i++) {
            deletedKeychains.add(deletedKeychainsArray.getJsonNumber(i).longValue());
        }
    }

    public JsonObjectBuilder dump() {
        JsonArrayBuilder keychainArray = Json.createArrayBuilder();
        for (DirectoryEntry keychain : keychains) {
            keychainArray.add(keychain.dump());
        }

        JsonArrayBuilder deletedKeychainsArray = Json.createArrayBuilder();
        for (Long deletedKeychain : deletedKeychains) {
            deletedKeychainsArray.add(deletedKeychain);
        }

        return Json.createObjectBuilder()
                .add("version", "1.0.0")
                .add("account", account.dump())
                .add("keychains", keychainArray)
                .add("deleted", deletedKeychainsArray);
    }

    public List<DirectoryEntry> getKeychains() {
        return keychains;
    }

    public boolean createKeychain(SecretKey encryptionKey, String name) {
        LOGGER.info("Creating keychain: " + name);

        DirectoryEntry entry = new DirectoryEntry(
                name,
                encryptionKey,
                this,
                backingStore
        );

        if (keychains.stream().map(k -> k.name).anyMatch(n -> n.equals(name))) {
            LOGGER.warning("Keychain " + name +  "is a duplicate, not adding.");
            return false;
        }

        this.keychains.add(entry);

        return true;
    }

    public DirectoryEntry createKeychain(JsonObject entryObject, JsonObject keychainObject) {
        DirectoryEntry entry = new DirectoryEntry(entryObject, this, backingStore);
        entry.keychain = new Keychain(keychainObject, entry);
        this.keychains.add(entry);

        return entry;
    }

    public boolean delete(DirectoryEntry keychain) {
        if (keychain.serverid != -1L) {
            deletedKeychains.add(keychain.serverid);
        }

        this.keychains.remove(keychain);
        return this.backingStore.deleteKeychain(keychain.name);
    }
}
