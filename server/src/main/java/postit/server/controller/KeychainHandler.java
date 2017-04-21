package postit.server.controller;

import org.json.JSONObject;
import postit.server.model.ServerKeychain;

import java.util.List;
import java.util.Objects;

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
        JSONObject response = db.addDirectoryEntry(username, -1, null, false, name, "");
        return checkResponse(response);
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

        boolean isOwner = entry.getOwnerUsername().equals(username);
        boolean isShared = Objects.equals(entry.getSharedUsername(), username);
        if (!(isOwner || (isShared && entry.isSharedHasWritePermission()))) {
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

    public JSONObject shareKeychain(String ownerUsername, String sharedUsername, boolean sharedCanWrite, long id) {
        ServerKeychain keychain = getKeychain(ownerUsername, id);

        if (keychain == null) {
            JSONObject res = new JSONObject();
            res.put("status", "failure");
            return res;
        }

        for (ServerKeychain serverKeychain : getSharedKeychains(ownerUsername, id)) {
            if (serverKeychain.getSharedUsername().equals(sharedUsername)) {
                JSONObject res = new JSONObject();
                res.put("status", "success");
                res.put("directoryEntryId", serverKeychain.getDirectoryEntryId());
                return res;
            }
        }

        JSONObject response = db.addDirectoryEntry(
                ownerUsername,
                keychain.getDirectoryEntryId(),
                sharedUsername,
                sharedCanWrite,
                keychain.getName(),
                keychain.getData()
        );

        return checkResponse(response);
    }

    private static JSONObject checkResponse(JSONObject response) {
        if (response.getString("status").equals("success")) {
            return response;
        } else {
            JSONObject res = new JSONObject();
            res.put("status", "failure");
            return res;
        }
    }

    public boolean removeKeychain(String username, long directoryEntryId) {
        ServerKeychain entry = db.getDirectoryEntry(directoryEntryId);

        if (entry == null) {
            return false;
        }

        boolean isOwner = entry.getOwnerUsername().equals(username);
        boolean isShared = Objects.equals(entry.getSharedUsername(), username);

        if (isOwner) {
            List<ServerKeychain> sharedKeychains = this.getSharedKeychains(username, directoryEntryId);
            for (ServerKeychain sharedKeychain : sharedKeychains) {
                if (!db.removeDirectoryEntry(sharedKeychain.getDirectoryEntryId())) {
                    System.err.println("Failed to delete dependent keychain of parent. Aborting.");
                    return false;
                }
            }
        }

        if (isOwner || isShared) {
            return db.removeDirectoryEntry(directoryEntryId);
        } else {
            return false;
        }
    }

    public ServerKeychain getKeychain(String username, long directoryEntryId) {
        ServerKeychain entry = db.getDirectoryEntry(directoryEntryId);

        if (entry == null) {
            return null;
        }

        boolean isOwner = entry.getOwnerUsername().equals(username);
        boolean isShared = Objects.equals(entry.getSharedUsername(), username);

        if (isOwner || isShared) {
            return entry;
        } else {
            return null;
        }
    }

    public List<ServerKeychain> getSharedKeychains(String username, Long id) {
        return db.getSharedInstancesOfDirectoryEntry(username, id);
    }

    public ServerKeychain getOwnersKeychain(String username, Long id) {
        ServerKeychain ownKeychain = this.getKeychain(username, id);

        if (ownKeychain == null) {
            return null;
        }

        if (Objects.equals(ownKeychain.getOwnerUsername(), username)) {
            return ownKeychain;
        } else {
            return this.getKeychain(ownKeychain.getOwnerUsername(), ownKeychain.getOwnerDirectoryEntryId());
        }

    }

    public boolean setSharedKeychainWriteable(String username, long id, String sharedUsername, boolean writeable) {
        ServerKeychain entry = db.getDirectoryEntry(id);

        if (entry == null) {
            return false;
        }

        boolean isOwner = entry.getOwnerUsername().equals(username);

        if (isOwner) {
            return db.setSharedKeychainWriteable(id, sharedUsername, writeable);
        } else {
            return false;
        }
    }

    public List<ServerKeychain> getKeychains(String username) {
        return db.getDirectoryEntries(username);
    }
}
