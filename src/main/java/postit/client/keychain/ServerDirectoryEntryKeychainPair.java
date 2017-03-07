package postit.client.keychain;

import javax.json.JsonObject;

/**
 * Created by nishadmathur on 7/3/17.
 */
public class ServerDirectoryEntryKeychainPair {
    DirectoryEntry entry;

    public ServerDirectoryEntryKeychainPair(DirectoryEntry entry) {
        this.entry = entry;
    }

    public ServerDirectoryEntryKeychainPair(Directory directory, JsonObject pair) {

    }
}
