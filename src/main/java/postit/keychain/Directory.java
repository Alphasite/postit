package postit.keychain;

import postit.backend.BackingStore;
import postit.backend.KeyService;

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

    List<DirectoryEntry> keychains;

    public Directory(KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
    }

    public Directory(JsonObject object, KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();

        JsonArray keychainArray = object.getJsonArray("keychains");
        for (int i = 0; i < keychainArray.size(); i++) {
            keychains.add(new DirectoryEntry(keychainArray.getJsonObject(i), this, keyService, backingStore));
        }
    }

    public JsonObjectBuilder dump() {
        JsonArrayBuilder keychainArray = Json.createArrayBuilder();
        for (DirectoryEntry keychain : keychains) {
            keychainArray.add(keychain.dump());
        }

        return Json.createObjectBuilder()
                .add("version", "1.0.0")
                .add("keychains", keychainArray);
    }

    public List<DirectoryEntry> getKeychains() {
        return keychains;
    }

    public Optional<Keychain> createKeychain(SecretKey encryptionKey, String name) {
        DirectoryEntry entry = new DirectoryEntry(
                name,
                encryptionKey,
                this,
                keyService,
                backingStore
        );

        Keychain keychain = new Keychain(name, entry);
        this.keychains.add(entry);

        if (entry.save()) {
            if (this.save()) {
                return Optional.of(keychain);
            } else {
                if (!entry.delete()) {
                    LOGGER.severe("Failed to delete entry after fialing to save directory, please clean up :" + entry.getPath());
                }

                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean delete(DirectoryEntry keychain) {
        return keychain.delete();
    }

    public boolean save() {
        return backingStore.writeDirectory(this);
    }
}
