package keychain;

import backend.BackingStore;
import backend.KeyService;

import javax.crypto.SecretKey;
import javax.json.*;
import java.util.*;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Directory {
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
            return Optional.of(keychain);
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
