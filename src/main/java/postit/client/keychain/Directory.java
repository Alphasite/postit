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

    BackingStore backingStore;
    KeyService keyService;

    public List<DirectoryEntry> keychains;
    public List<Long> deletedKeychains;

    public Directory(KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();
    }

    public Directory(JsonObject object, KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();

        JsonArray keychainArray = object.getJsonArray("keychains");
        for (int i = 0; i < keychainArray.size(); i++) {
            keychains.add(new DirectoryEntry(keychainArray.getJsonObject(i), this, keyService, backingStore));
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
                .add("keychains", keychainArray)
                .add("deleted", deletedKeychainsArray);
    }

    public List<DirectoryEntry> getKeychains() {
        return keychains;
    }

    public Optional<Keychain> createKeychain(SecretKey encryptionKey, String name) {
        LOGGER.info("Creating keychain: " + name);

        DirectoryEntry entry = new DirectoryEntry(
                name,
                encryptionKey,
                this,
                keyService,
                backingStore
        );

        Keychain keychain = new Keychain(name, entry);

        if (keychains.stream().map(k -> k.name).anyMatch(n -> n.equals(name))) {
            LOGGER.warning("Keychain " + name +  "is a duplicate, not adding.");
            return Optional.empty();
        }

        this.keychains.add(entry);

        if (entry.save()) {
            if (this.save()) {
                return Optional.of(keychain);
            } else {
                if (!entry.delete()) {
                    LOGGER.severe("Failed to delete entry after failing to save directory, please clean up :" + entry.getPath());
                }

                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<DirectoryEntry> createKeychain(JsonObject entryObject, JsonObject keychainObject) {
        DirectoryEntry entry = new DirectoryEntry(entryObject, this, keyService, backingStore);
        entry.keychain = new Keychain(keychainObject, entry);
        this.keychains.add(entry);


        if (entry.save()) {
            if (this.save()) {
                return Optional.of(entry);
            } else {
                if (!entry.delete()) {
                    LOGGER.severe("Failed to delete entry after failing to save directory, please clean up :" + entry.getPath());
                }

                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean delete(DirectoryEntry keychain) {
        if (keychain.serverid != 0L) {
            deletedKeychains.add(keychain.serverid);
        }

        return keychain.delete();
    }

    public boolean save() {
        LOGGER.info("Saving directory");
        return backingStore.writeDirectory(this);
    }


}
