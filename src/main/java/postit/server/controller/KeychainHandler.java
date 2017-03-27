package postit.server.controller;

import java.util.List;

import org.json.JSONObject;

import postit.server.model.*;

/**
 * Class handling requests from frontend and directs to the proper backend controller.
 * Changes needed in future:
 * - change metadata's type
 * - an interface for Handler
 * - event response JSONObject or CODE instead of boolean
 * - privatize the handling functions
 *
 * @author Ning
 */
public class KeychainHandler {

    private DatabaseController db;

    public KeychainHandler(DatabaseController db) {
        this.db = db;
    }


    public JSONObject createKeychain(String username, String name) {
        JSONObject response = db.addDirectoryEntry(username, name, "");

        if (response.getString("status").equals("success")) {
            return response;
        } else {
            JSONObject res = new JSONObject();
            res.put("status", "failure");
            return res;
        }
    }

    /**
     * Updates keychain information in the database. Any item that should be left unchanged should be null.
     *
     * @param directoryEntryId
     * @param name
     * @return
     */
    public boolean updateKeychain(String username, long directoryEntryId, String name, String data) {
        ServerKeychain entry = db.getDirectoryEntry(directoryEntryId);

        if (!entry.getOwnerUsername().equals(username)) {
            return false;
        }

        if (name != null) {
            entry.setName(name);
        }

        if (data != null) {
            entry.setData(data);
        }

        return db.updateDirectoryEntry(entry);
    }

    public boolean updateKeychain(String username, ServerKeychain keychain) {
        return updateKeychain(username, keychain.getDirectoryEntryId(), keychain.getName(), keychain.getData());
    }

    public boolean removeKeychain(long directoryEntryId) {
        return db.removeDirectoryEntry(directoryEntryId);
    }

    public ServerKeychain getKeychain(String username, int directoryEntryId) {
        ServerKeychain entry = db.getDirectoryEntry(directoryEntryId);

        if (entry != null && entry.getOwnerUsername().equals(username)) {
            return entry;
        } else {
            return null;
        }
    }

    public ServerKeychain getKeychain(String username, String name) {
        return db.getDirectoryEntry(username, name);
    }

    public List<ServerKeychain> getKeychains(String username) {
        return db.getDirectoryEntries(username);
    }
}
