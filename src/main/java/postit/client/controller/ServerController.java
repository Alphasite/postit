package postit.client.controller;

import postit.client.keychain.Account;
import postit.client.keychain.DirectoryEntry;
import postit.shared.communication.Client;
import postit.shared.model.DirectoryAndKey;

import javax.json.*;
import java.text.MessageFormat;
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

    private Client clientToServer;
    private DirectoryController directoryController;

    private Thread syncThread;

    public ServerController(Client client) {
        this.clientToServer = client;
        this.directoryController = null;
        this.syncThread = null;
    }

    public boolean sync(Runnable callback) {

        Runnable sync = () -> {
            LOGGER.info("Entering sync...");
            Account account = directoryController.getAccount();
            this.authenticate(account); // TODO

            Set<Long> serverKeychains = new HashSet<>(this.getKeychains());

            List<DirectoryEntry> clientKeychains = directoryController.getKeychains();

            Set<Long> clientKeychainNames = clientKeychains.stream()
                    .map(keychain -> keychain.serverid)
                    .collect(Collectors.toSet());

            // Figure out which keychains have been uploaded to the server and are no longer there.
            Set<Long> serverDeletedKeychains = clientKeychains.stream()
                            .map(keychain -> keychain.serverid)
                            .filter(id -> id != -1)
                            .collect(Collectors.toSet());
            serverDeletedKeychains.removeAll(serverKeychains);

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

            System.out.println(MessageFormat.format(
                    "Total [remote: {}, local: {}] Deleting [remote: {}, local: {}] Downloading {} Uploading {} update {}",
                    serverKeychains.size(),
                    clientKeychains.size(),
                    serverKeychainsToDelete.size(),
                    localKeychainsToDelete.size(),
                    keychainsToDownload.size(),
                    keychainsToUpload.size(),
                    keychainsToUpdate.size()
            ));

            System.out.println("Server keychains: " + serverKeychains);
            System.out.println("Client keychains: " + clientKeychainNames);

            for (Long serverid : serverKeychainsToDelete) {
                if (!deleteKeychain(serverid)) {
                    LOGGER.warning("Failed to delete keychain (" + serverid + ") from server.");
                    return;
                }
            }

            for (DirectoryEntry entry : clientKeychains) {
                if (!createKeychain(entry)) {
                    LOGGER.warning("Failed to upload keychain (" + entry.name + ") to server.");
                    return;
                }
            }

            for (Long serverid : keychainsToDownload) {
                JsonObject directoryKeychainObject = getDirectoryKeychainObject(serverid);

                if (directoryKeychainObject != null) {
                    if (!directoryController.createKeychain(
                            directoryKeychainObject.getJsonObject("entry"),
                            directoryKeychainObject.getJsonObject("keychain") // TODO encrypt decrypt??
                    )) {
                        LOGGER.warning("Failed to merge keychain (" + serverid + ") merge keychain.");
                        return;
                    }
                } else {
                    LOGGER.warning("Failed to download keychain (" + serverid + ") from server.");
                    return;
                }
            }

            for (DirectoryEntry entry : new ArrayList<>(directoryController.getKeychains())) {
                if (localKeychainsToDelete.contains(entry.serverid)) {
                    directoryController.deleteEntry(entry);
                    continue;
                }

                if (keychainsToUpdate.contains(entry.serverid)) {
                    JsonObject directoryKeychainObject = getDirectoryKeychainObject(entry.serverid);

                    directoryController.updateLocalIfIsOlder(
                            entry,
                            directoryKeychainObject.getJsonObject("entry"),
                            directoryKeychainObject.getJsonObject("keychain") // TODO encrypt decrypt??
                    );

                    if (setKeychain(entry)) {
                        continue;
                    } else {
                        LOGGER.warning("Failed to update keychain (" + entry.name + ")  on server...");
                        return;
                    }
                }
            }

            callback.run();
        };

        if (syncThread == null) {
            syncThread = new Thread(sync);
            syncThread.run();
            return true;
        } else {
            return false;
        }
    }

    public boolean addUser(Account account, String email, String firstname, String lastname) {
        String req = RequestMessenger.createAddUserMessage(account, email, firstname, lastname);
        return sendAndCheckIfSuccess(req);
    }

    public boolean authenticate(Account account) {
        String req = RequestMessenger.createAuthenticateMessage(account);
        return sendAndCheckIfSuccess(req);
    }

    private List<Long> getKeychains() {
        String req = RequestMessenger.createGetKeychainsMessage(directoryController.getAccount());
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent()) {
            JsonArray list = response.get().getJsonArray("keychains");
            List<Long> keys = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                JsonObject key = list.getJsonObject(i);
                if (key.containsKey("directoryEntryId"))
                    keys.add((long) key.getInt("directoryEntryId"));
            }
            return keys;
        } else {
            return null;
        }
    }

    private JsonObject getDirectoryKeychainObject(long serverid) {
        String req = RequestMessenger.createGetKeychainMessage(directoryController.getAccount(), serverid);
        Optional<JsonObject> response = clientToServer.send(req);

        return response.orElse(null);
    }

    private boolean createKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        // TODO fill this in?
        String req = RequestMessenger.createAddKeychainsMessage(directoryController.getAccount(), entry.name, entry.getEncryptionKey().toString(), "", "");
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent()) {
            DirectoryAndKey dak = DirectoryAndKey.fromJsonObject(response.get().getJsonObject("keychain"));
            long id = dak.getDirectoryEntryId();

            // ask server for new keychain id;
            Optional<Long> newid = Optional.of(id);

            if (!newid.isPresent()) {
                LOGGER.warning("Failed to create keychain (" + entry.name + ")  on server...");
                return false;
            }

            directoryController.setKeychainOnlineId(entry, newid.get());

            return setKeychain(entry);
        } else {
            return false;
        }
    }

    public boolean setDirectoryController(DirectoryController d) {
        this.directoryController = d;
        return true;
    }

    private boolean setKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        // TODO fill this in?
        String req = RequestMessenger.createUpdateKeychainMessage(directoryController.getAccount(), entry.name, entry.getEncryptionKey().toString(), "", "");
        return sendAndCheckIfSuccess(req);
    }

    private boolean deleteKeychain(long id) {
        String req = RequestMessenger.createRemoveKeychainMessage(directoryController.getAccount(), id);
        return sendAndCheckIfSuccess(req);
    }

    private boolean sendAndCheckIfSuccess(String req) {
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent()) {
            JsonObject res = response.get();
            return res.getString("status").equals("success");
        } else {
            return false;
        }
    }
}
