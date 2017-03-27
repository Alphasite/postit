package postit.client.controller;

import postit.client.keychain.Account;
import postit.client.keychain.DirectoryEntry;
import postit.server.model.ServerKeychain;
import postit.shared.communication.Client;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.StringReader;
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

            List<Long> retrievedServerKeychainIds = this.getKeychains();

            if (retrievedServerKeychainIds == null) {
                LOGGER.warning("Failed to sync, server returned no keychains.");
                return;
            }

            Set<Long> serverKeychains = new HashSet<>(retrievedServerKeychainIds);

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

            // Figure out which keychains to download from the server
            Set<Long> keychainsToDownload = new HashSet<>(serverKeychains);
            keychainsToDownload.removeAll(clientKeychainNames);
            keychainsToDownload.removeAll(serverKeychainsToDelete);

            // Figure out which keychains to upload to the server
            Set<Long> keychainsToUpload = new HashSet<>(clientKeychainNames);
            keychainsToUpload.removeAll(serverKeychains);
            keychainsToUpload.removeAll(localKeychainsToDelete);

            // Figure out which keychains may need an update, and update them.
            Set<Long> keychainsToUpdate = new HashSet<>(clientKeychainNames);
            keychainsToUpdate.retainAll(serverKeychains);

            System.out.println(MessageFormat.format(
                    "Total [remote: {0}, local: {1}] Deleting [remote: {2}, local: {3}] Downloading {4} Uploading {5} update {6}",
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
                Optional<JsonObject> directoryKeychainObject = getDirectoryKeychainObject(serverid);

                if (directoryKeychainObject.isPresent()) {
                    if (!directoryController.createKeychain(
                            directoryKeychainObject.get().getJsonObject("entry"),
                            directoryKeychainObject.get().getJsonObject("keychain") // TODO encrypt decrypt??
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
                    Optional<JsonObject> directoryKeychainObject = getDirectoryKeychainObject(entry.serverid);

                    if (!directoryKeychainObject.isPresent()) {
                        LOGGER.warning("Failed to fetch keychain for update (" + entry.name + ").");
                        return;
                    }

                    directoryController.updateLocalIfIsOlder(
                            entry,
                            directoryKeychainObject.get().getJsonObject("entry"),
                            directoryKeychainObject.get().getJsonObject("keychain") // TODO encrypt decrypt??
                    );

                    if (!setKeychain(entry)) {
                        LOGGER.warning("Failed to update keychain (" + entry.name + ")  on server...");
                        return;
                    }
                }
            }

            callback.run();
            LOGGER.info("Sync complete.");
        };

        if (syncThread == null || !syncThread.isAlive()) {
            syncThread = new Thread(sync);
            syncThread.start();
            return true;
        } else {
            return false;
        }
    }

    public boolean addUser(Account account, String email, String firstname, String lastname) {
        String req = RequestMessenger.createAddUserMessage(account, email, firstname, lastname);
        return sendAndCheckIfSuccess(req);
    }

    public boolean removeUser(Account account){
    	String req = RequestMessenger.createRemoveUserMessage(account);
    	return sendAndCheckIfSuccess(req);
    }
    
    public boolean authenticate(Account account) {
        String req = RequestMessenger.createAuthenticateMessage(account);
        return sendAndCheckIfSuccess(req);
    }

    public List<Long> getKeychains() {
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

    private Optional<JsonObject> getDirectoryKeychainObject(long serverid) {
        String req = RequestMessenger.createGetKeychainMessage(directoryController.getAccount(), serverid);
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent() && response.get().getString("status").equals("success")) {
            String keychainString = new String(Base64.getDecoder().decode(response.get().getString("keychain")));

            try {
                return Optional.ofNullable(Json.createReader(new StringReader(keychainString)).readObject());
            } catch (JsonException | IllegalStateException e) {
                LOGGER.warning("Failed to parse server keychain response.");
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean createKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        String encodedKeychainEntryObject = Base64.getEncoder().encodeToString(keychainEntryObject.get().build().toString().getBytes());

        String req = RequestMessenger.createAddKeychainsMessage(
                directoryController.getAccount(),
                entry.name,
                encodedKeychainEntryObject
        );

        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent() && response.get().getString("status").equals("success")) {
            ServerKeychain serverKeychain = new ServerKeychain(response.get().getJsonObject("keychain"));
            long id = serverKeychain.getDirectoryEntryId();

            directoryController.setKeychainOnlineId(entry, id);

            return setKeychain(entry);
        } else {
            return false;
        }
    }

    public boolean setDirectoryController(DirectoryController d) {
        this.directoryController = d;
        return true;
    }

    public boolean setKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        String encodedKeychainEntryObject = Base64.getEncoder().encodeToString(keychainEntryObject.get().build().toString().getBytes());

        // TODO fill this in?
        String req = RequestMessenger.createUpdateKeychainMessage(
                directoryController.getAccount(),
                entry.serverid,
                entry.name,
                encodedKeychainEntryObject
        );

        return sendAndCheckIfSuccess(req);
    }

    public boolean deleteKeychain(long id) {
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
