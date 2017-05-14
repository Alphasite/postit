package postit.client.controller;

import postit.client.backend.KeyService;
import postit.client.communication.Client;
import postit.client.keychain.Account;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.DirectoryKeychain;
import postit.client.keychain.Share;
import postit.server.model.ServerKeychain;
import postit.shared.MessagePackager;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static postit.shared.MessagePackager.Asset.*;
import static postit.shared.MessagePackager.typeToString;

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
    private KeyService keyService;

    private Thread syncThread;

    public void setKeyService(KeyService keyService) {
        this.keyService = keyService;
    }

    public ServerController(Client client) {
        this.clientToServer = client;
        this.directoryController = null;
        this.keyService = null;
        this.syncThread = null;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean sync(Runnable callback) {

        Runnable sync = () -> {
            Optional<Account> account = directoryController.getAccount();

            if (!account.isPresent()) {
                LOGGER.warning("Not logged in. Aborting.");
            }

            LOGGER.info("Entering sync...");

            List<Long> retrievedServerKeychainIds = this.getKeychains(account.get());

            if (retrievedServerKeychainIds == null) {
                LOGGER.warning("Failed to sync, server returned no keychains.");
                return;
            }

            Set<Long> serverKeychains = new HashSet<>(retrievedServerKeychainIds);

            List<DirectoryEntry> clientKeychains = directoryController.getKeychains();

            Set<Long> clientKeychainNames = clientKeychains.stream()
                    .map(keychain -> keychain.getServerId())
                    .collect(Collectors.toSet());

            // Figure out which keychains have been uploaded to the server and are no longer there.
            Set<Long> serverDeletedKeychains = clientKeychains.stream()
                            .map(keychain -> keychain.getServerId())
                            .filter(id -> id != -1)
                            .collect(Collectors.toSet());
            serverDeletedKeychains.removeAll(serverKeychains);

            // Figure out which keychains are here but deleted on the server
            Set<Long> localKeychainsToDelete = new HashSet<>(serverDeletedKeychains);
            localKeychainsToDelete.retainAll(clientKeychainNames);

            // Figure out which keychains have been deleted here
            Set<Long> serverKeychainsToDelete = new HashSet<>(serverKeychains);
            serverKeychainsToDelete.retainAll(directoryController.getDeletedKeychains());

            // Figure out which keychains to download from the server
            Set<Long> keychainsToDownload = new HashSet<>(serverKeychains);
            keychainsToDownload.removeAll(clientKeychainNames);
            keychainsToDownload.removeAll(serverKeychainsToDelete);

            // Figure out which keychains to upload to the server
            List<DirectoryEntry> keychainsToUpload = clientKeychains.stream()
                    .filter(keychain -> keychain.getServerId() == -1)
                    .collect(Collectors.toList());

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

            // Delete keychains on server which have been deleted on the client
            for (Long serverid : serverKeychainsToDelete) {
                if (!deleteKeychain(account.get(), serverid)) {
                    LOGGER.warning("Failed to delete keychain (" + serverid + ") from server.");
                    return;
                }
            }

            // Upload newly created keychains
            for (DirectoryEntry entry : keychainsToUpload) {
                if (entry.getOwner() == null) {
                    entry.setOwner(account.get().getUsername());
                }

                if (!createKeychain(account.get(), entry)) {
                    LOGGER.warning("Failed to upload keychain (" + entry.name + ") to server.");
                    return;
                }
            }

            // Download fresh keychains which the client has permission to access.
            for (Long serverid : keychainsToDownload) {
                Optional<DirectoryKeychain> directoryKeychain = getOwnerDirectoryKeychainObject(account.get(), serverid, Optional.empty());

                if (directoryKeychain.isPresent()) {
                    if (!directoryController.createKeychain(
                            serverid,
                            directoryKeychain.get().entry,
                            directoryKeychain.get().keychain
                    )) {
                        LOGGER.warning("Failed to merge keychain (" + serverid + ") merge keychain.");
                        return;
                    }
                } else {
                    LOGGER.warning("Failed to download keychain (" + serverid + ") from server.");
                    return;
                }
            }

            // This is the meat of the sync function
            // It handles downloading and merging server copies of the keychains.
            for (DirectoryEntry entry : new ArrayList<>(directoryController.getKeychains())) {
                if (localKeychainsToDelete.contains(entry.getServerId())) {
                    directoryController.deleteEntry(entry);
                    continue;
                }

                if (keychainsToUpdate.contains(entry.getServerId())) {
                    Optional<Share> any = entry.shares.stream()
                            .filter(share -> share.isOwner)
                            .findAny();

                    // Merge owner first.
                    Optional<DirectoryKeychain> ownerDirectoryKeychainObject = this.getOwnerDirectoryKeychainObject(
                            account.get(),
                            entry.getServerId(),
                            Optional.of(any.get().signatureKey)
                    );

                    directoryController.updateLocalIfIsOlder(
                            entry,
                            ownerDirectoryKeychainObject.get().entry,
                            ownerDirectoryKeychainObject.get().keychain,
                            account.get().getUsername(),
                            true
                    );

                    Share ownerShare = entry.shares.stream()
                            .filter(s -> s.isOwner)
                            .findAny()
                            .get();

                    boolean isOwnerOfEntry = ownerShare.serverid == entry.getServerId();

                    // Get all non-owner instances of this keychain.
                    Optional<List<DirectoryKeychain>> allInstancesOfKeychain = getAllAccessibleInstances(account.get(), entry);

                    if (!allInstancesOfKeychain.isPresent()) {
                        LOGGER.warning("Failed to fetch keychains for update (" + entry.name + ").");
                        return;
                    }

                    // Get the ids of all the shared copies of the keychain
                    Set<Long> sharedKeychainsOnServer = allInstancesOfKeychain.get().stream()
                            .map(DirectoryKeychain::getServerId)
                            .collect(Collectors.toSet());

                    // Iterate through each of them
                    for (DirectoryKeychain directoryKeychain : allInstancesOfKeychain.get()) {
                        // Find the share for this entry.
                        Optional<Share> share = entry.shares.stream()
                                    .filter(s -> s.serverid == directoryKeychain.getServerId())
                                    .findAny();

                        if (!share.isPresent()) {
                            // shouldnt be here, but is.
                            LOGGER.severe("[Owner: Merging shared keychains] System has a shared keychain with no corresponding local data?");
                            continue;
                        }

                        // Allow if owner can write or is self.
                        if (!share.get().canWrite && share.get().serverid != entry.getServerId()) {
                            continue;
                        }

                        // Merge in the keychain
                        directoryController.updateLocalIfIsOlder(
                            entry,
                            directoryKeychain.entry,
                            directoryKeychain.keychain,
                            share.get().username,
                            false
                        );
                    }

                    if (isOwnerOfEntry) {
                        // decide if need to share/unshare the keychain
                        for (Share share : new ArrayList<>(entry.shares)) {
                            if (!share.isOwner) {
                                if (share.serverid != -1 && !sharedKeychainsOnServer.contains(share.serverid)) {
                                    directoryController.unshareKeychain(entry, share);
                                }

                                if (share.serverid == -1) {
                                    if (!this.shareKeychain(account.get(), entry, share)) {
                                        LOGGER.warning("Filed to share keychain: " + entry.name + " user: " + share.username + " does nto exist. Deleting share.");
                                        directoryController.unshareKeychain(entry, share);
                                    } else {
                                        LOGGER.info("Succeed to share keychain: " + entry.name + " user: " + share.username + " id: " + share.serverid);
                                    }
                                }
                            }
                        }

                        sharedKeychainsOnServer = allInstancesOfKeychain.get().stream()
                                .map(DirectoryKeychain::getServerId)
                                .collect(Collectors.toSet());

                        Set<Long> sharedKeychainsTheClientKnowsOf = entry.shares.stream()
                                .map(e -> e.serverid)
                                .collect(Collectors.toSet());

                        sharedKeychainsOnServer.removeAll(sharedKeychainsTheClientKnowsOf);

                        // Remove keychains which have been unshared.
                        for (Long serverid : sharedKeychainsOnServer) {
                            deleteKeychain(account.get(), serverid);
                        }
                    }

                    if (!setKeychain(account.get(), entry)) {
                        LOGGER.warning("Failed to update keychain (" + entry.name + ")  on server...");
                        return;
                    }
                }
            }

            directoryController.save();

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

    public boolean addUser(Account account, String email, String firstname, String lastname, String phoneNumber, String keypair, String publicKey) {
        String req = RequestMessenger.createAddUserMessage(account, email, firstname, lastname, phoneNumber, keypair, publicKey);
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

    public String getPhoneNumber(Account account){
    	String req = RequestMessenger.createGetUserMessage(account);
    	Optional<JsonObject> response = clientToServer.send(req);
    	if (response.isPresent()) {
    		return response.get().getJsonObject("account").getString("phoneNumber");
    	}
    	
    	return null;
    }
    
    public List<Long> getKeychains(Account account) {
        String req = RequestMessenger.createGetKeychainsMessage(account);
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent()) {
            JsonArray list = response.get().getJsonArray("keychains");
            List<Long> keys = new ArrayList<>();

            if (list == null) {
                LOGGER.warning("Failed to fetch keychain: " + response.get().getString("message"));
                return null;
            }

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

    public Optional<DirectoryKeychain> getDirectoryKeychainObject(Account account, long serverid) {
        String req = RequestMessenger.createGetKeychainMessage(account, serverid);
        Optional<JsonObject> response = clientToServer.send(req);
        return parseDirectoryKeychainResponse(account, response, KEYCHAIN, Optional.of(account.getSigningKeypair().getPublic()));
    }

    public Optional<DirectoryKeychain> getOwnerDirectoryKeychainObject(Account account, long serverid, Optional<PublicKey> signingKey) {
        String req = RequestMessenger.createGetOwnerKeychainMessage(account, serverid);
        Optional<JsonObject> response = clientToServer.send(req);
        return parseDirectoryKeychainResponse(account, response, OWNER_KEYCHAIN, signingKey);
    }

    public static Optional<DirectoryKeychain> parseDirectoryKeychainResponse(Account account, Optional<JsonObject> response, MessagePackager.Asset asset, Optional<PublicKey> signingKey) {
        if (response.isPresent() && response.get().getString("status").equals("success")) {
            try {
                JsonObject keychain = response.get().getJsonObject(typeToString(asset));
                String decodedDirectoryKeychain = new String(Base64.getDecoder().decode(keychain.getString("data")), StandardCharsets.UTF_8);
                JsonObject object = Json.createReader(new StringReader(decodedDirectoryKeychain)).readObject();
                return DirectoryKeychain.init(
                        keychain.getJsonNumber("directoryEntryId").longValue(),
                        object,
                        account,
                        signingKey
                );
            } catch (JsonException | IllegalStateException e) {
                LOGGER.warning("Failed to parse server keychain response: " + e.getMessage());
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean createKeychain(Account account, DirectoryEntry entry) {
        Optional<JsonObject> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        String encodedKeychainEntryObject = Base64.getEncoder().encodeToString(keychainEntryObject.get().toString().getBytes(StandardCharsets.UTF_8));

        String req = RequestMessenger.createAddKeychainsMessage(
                account,
                entry.name,
                encodedKeychainEntryObject
        );

        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent() && response.get().getString("status").equals("success")) {
            ServerKeychain serverKeychain = new ServerKeychain(response.get().getJsonObject("keychain"));
            long id = serverKeychain.getDirectoryEntryId();

            directoryController.setKeychainOnlineId(entry, id);

            return setKeychain(account, entry);
        } else {
            return false;
        }
    }

    public boolean setDirectoryController(DirectoryController d) {
        this.directoryController = d;
        return true;
    }

    public boolean setKeychain(Account account, DirectoryEntry entry) {
        Optional<JsonObject> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        String encodedKeychainEntryObject = Base64.getEncoder().encodeToString(keychainEntryObject.get().toString().getBytes(StandardCharsets.UTF_8));

        // TODO fill this in?
        String req = RequestMessenger.createUpdateKeychainMessage(
                account,
                entry.getServerId(),
                entry.name,
                encodedKeychainEntryObject
        );

        return sendAndCheckIfSuccess(req);
    }

    public boolean deleteKeychain(Account account, long id) {
        String req = RequestMessenger.createRemoveKeychainMessage(account, id);
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

    public boolean shareKeychain(Account account, DirectoryEntry entry, Share share) {
        String req = RequestMessenger.createSharedKeychainMessage(account, entry.getServerId(), share.username, share.canWrite);
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent() && response.get().getString("status").equals("success")) {
            long id = response.get()
                    .getJsonObject(typeToString(SHARED_KEYCHAIN))
                    .getJsonNumber("directoryEntryId")
                    .longValue();

            return directoryController.setKeychainSharedId(entry, id, share);
        } else {
            return false;
        }
    }

    public Optional<List<DirectoryKeychain>> getAllAccessibleInstances(Account account, DirectoryEntry entry) {
        String req = RequestMessenger.createGetKeychainInstancesMessage(account, entry.getServerId());
        Optional<JsonObject> response = clientToServer.send(req);

        List<DirectoryKeychain> directoryKeychains = new ArrayList<>();

        if (response.isPresent()) {
            JsonArray list = response.get().getJsonArray(typeToString(SHARED_KEYCHAINS));

            if (list == null) {
                LOGGER.warning("Failed to fetch keychains: " + response.get().getString("message"));
                return Optional.empty();
            }

            for (int i = 0; i < list.size(); i++) {
                JsonObject keychain = list.getJsonObject(i);

                try {
                    long directoryEntryId = keychain.getJsonNumber("directoryEntryId").longValue();
                    Optional<Share> currentShareOfEntry = entry.shares.stream()
                            .filter(share -> share.serverid == directoryEntryId)
                            .findAny();

                    if (!currentShareOfEntry.isPresent()) {
                        LOGGER.warning("Directory entry " + entry.name + " with server id: " + directoryEntryId + " has no local share. skipping.");
                        continue;
                    }

                    String decodedDirectoryKeychain = new String(Base64.getDecoder().decode(keychain.getString("data")),StandardCharsets.UTF_8);
                    JsonObject object = Json.createReader(new StringReader(decodedDirectoryKeychain)).readObject();
                    Optional<DirectoryKeychain> directoryKeychain = DirectoryKeychain.init(
                            directoryEntryId,
                            object,
                            account,
                            Optional.of(currentShareOfEntry.get().signatureKey)
                    );

                    if (!directoryKeychain.isPresent()) {
                        LOGGER.warning("Failed to parse json object");
                        return Optional.empty();
                    }

                    directoryKeychains.add(directoryKeychain.get());

                } catch (JsonException | IllegalStateException e) {
                    LOGGER.warning("Failed to parse server keychain response: " + e.getMessage());
                    return Optional.empty();
                }
            }

            return Optional.of(directoryKeychains);
        } else {
            return Optional.empty();
        }
    }

    public boolean setUserCanWrite(Account account, Share share) {
        return sendAndCheckIfSuccess(RequestMessenger.createUpdateSharedKeychainMessage(
            account,
            share.serverid,
            share.username,
            share.canWrite
        ));
    }

    public boolean sendGetKeypairRequest(Account account) {
        return sendAndCheckIfSuccess(RequestMessenger.createGetKeypairMessage(account));
    }

    public boolean sendKeypairOtpResponse(Account account, String otp) {
        String req = RequestMessenger.sendOtpMessage(account, otp);
        Optional<JsonObject> response = clientToServer.send(req);

        if (response.isPresent() && response.get().getString("status").equals("success")) {
            JsonObject jsonObject = response.get().getJsonObject(typeToString(SHARED_KEYCHAIN));
            return account.deserialiseKeypairs(keyService.getMasterKey(), jsonObject);
        } else {
            return false;
        }
    }
}
