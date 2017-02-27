package Keychain;

import java.lang.UnsupportedOperationException;

import javax.crypto.SecretKey;
import javax.json.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Directory {
    List<DirectoryEntry> keychains;
    Path ownPath;

    public Directory(Path ownPath) {
        this.keychains = new ArrayList<>();
        this.ownPath = ownPath;
    }

    public Directory(JsonObject object, Path ownPath) {
        keychains = new ArrayList<>();

        JsonArray keychainArray = object.getJsonArray("keychains");
        for (int i = 0; i < keychainArray.size(); i++) {
            keychains.add(new DirectoryEntry(keychainArray.getJsonObject(i), ownPath));
        }
    }

    public JsonObjectBuilder dump() {
        JsonArrayBuilder keychainArray = Json.createArrayBuilder();
        for (DirectoryEntry keychain : keychains) {
            keychainArray.add(keychain.dump());
        }

        return Json.createObjectBuilder()
                .add("keychains", keychainArray);
    }

    public List<DirectoryEntry> getKeychains(SecretKey masterPassword) {
        return keychains;
    }

    public Keychain createKeychain(SecretKey encryptionKey, String name) {
        throw new UnsupportedOperationException();
    }

    public boolean writeKeychain(Keychain keychain) {
        throw new UnsupportedOperationException();
    }
}
