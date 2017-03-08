package postit.client.controller;

import postit.client.keychain.DirectoryEntry;
import postit.communication.Client;

import javax.crypto.SecretKey;
import javax.json.JsonObject;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by nishadmathur on 7/3/17.
 * <p>
 * POST /login/ - basic auth
 * GET /keychain/list
 * GET /keychain/list/deleted
 * GET /keychain/[serverid]
 * DELETE /keychain/[serverid]
 * GET /keychain/create - Get id for new keychain object.
 * POST /keychain/ - payload: keychain + directory as json
 */
public class ServerController {
    private final static Logger LOGGER = Logger.getLogger(ServerController.class.getName());

    Client client;
    DirectoryController directoryController;

    public ServerController(Client client, DirectoryController directoryController) {
        this.client = client;
        this.directoryController = directoryController;
    }

    public boolean sync() {
        this.login("", null);

        Set<Long> serverKeychains = new HashSet<>(this.getKeychains());
        List<Long> serverDeletedKeychains = getDeletedKeychains();

        List<DirectoryEntry> clientKeychains = directoryController.getKeychains();

        Set<Long> clientKeychainNames = clientKeychains.stream()
                .map(keychain -> keychain.serverid)
                .collect(Collectors.toSet());

        // Figure out which keychains are here but deleted on the server
        Set<Long> localKeychainsToDelete = new HashSet<>(serverDeletedKeychains);
        localKeychainsToDelete.retainAll(clientKeychainNames);

        // Figure out which keychains have been deleted here
        Set<Long> serverKeychainsToDelete = new HashSet<>(directoryController.getDeletedKeychains());
        serverKeychainsToDelete.removeAll(serverDeletedKeychains);

        Set<Long> keychainsToDownload = new HashSet<>(serverKeychains);
        keychainsToDownload.removeAll(clientKeychainNames);
        keychainsToDownload.removeAll(serverKeychainsToDelete);

        Set<Long> keychainsToUpload = new HashSet<>(clientKeychainNames);
        keychainsToUpload.removeAll(serverKeychains);
        keychainsToUpload.removeAll(localKeychainsToDelete);

        Set<Long> keychainsToUpdate = new HashSet<>(clientKeychainNames);
        keychainsToUpdate.retainAll(serverKeychains);

        for (Long serverid : serverKeychainsToDelete) {
            if (!deleteKeychain(serverid)) {
                LOGGER.warning("Failed to delete keychain (" + serverid + ") from server.");
                return false;
            }
        }

        for (DirectoryEntry entry : clientKeychains) {
            if (!createKeychain(entry)) {
                LOGGER.warning("Failed to upload keychain (" + entry.name + ") to server.");
                return false;
            }
        }

        for (Long serverid : keychainsToDownload) {
            JsonObject directoryKeychainObject = getKeychain(serverid);

            if (directoryKeychainObject != null) {
                if (!directoryController.createKeychain(
                        directoryKeychainObject.getJsonObject("entry"),
                        directoryKeychainObject.getJsonObject("keychain") // TODO encrypt decrypt??
                )) {
                    LOGGER.warning("Failed to merge keychain (" + serverid + ") merge keychain.");
                    return false;
                }
            } else {
                LOGGER.warning("Failed to download keychain (" + serverid + ") from server.");
                return false;
            }
        }

        for (DirectoryEntry entry : new ArrayList<>(directoryController.getKeychains())) {
            if (localKeychainsToDelete.contains(entry.serverid)) {
                directoryController.deleteEntry(entry);
                continue;
            }

            if (keychainsToUpdate.contains(entry.serverid)) {
                JsonObject directoryKeychainObject = getKeychain(entry.serverid);

                directoryController.updateLocalIfIsOlder(
                        entry,
                        directoryKeychainObject.getJsonObject("entry"),
                        directoryKeychainObject.getJsonObject("keychain") // TODO encrypt decrypt??
                );

                if (setKeychain(entry)) {
                    continue;
                } else {
                    LOGGER.warning("Failed to update keychain (" + entry.name + ")  on server...");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean login(String username, SecretKey password) {
        return false;
    }

    private List<Long> getKeychains() {
        // Make request to server for keychain's serverids
        return null;
    }

    private List<Long> getDeletedKeychains() {
        // Make request to server for list of deleted keychain's serverids
        return null;
    }

    private JsonObject getKeychain(long serverid) {
        // Make request to server for keychain with specific serverid
        return null;
    }

    private boolean createKeychain(DirectoryEntry entry) {
        // ask server for new keychain id;
        Optional<Long> newid = getNewKeychainId();

        if (!newid.isPresent()) {
            LOGGER.warning("Failed to create keychain (" + entry.name + ")  on server...");
            return false;
        }

        entry.serverid = newid.get();
        entry.save();

        return setKeychain(entry);
    }

    private boolean setKeychain(DirectoryEntry entry) {
        return false;
    }

    private boolean deleteKeychain(long id) {
        return false;
    }

    private Optional<Long> getNewKeychainId() {
        return Optional.empty();
    }
}
