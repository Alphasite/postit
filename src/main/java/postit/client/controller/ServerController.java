package postit.client.controller;

import postit.client.backend.KeyService;
import postit.client.keychain.DirectoryEntry;
import postit.communication.Client;
import postit.communication.Server;
import postit.shared.Crypto;
import postit.shared.model.DirectoryAndKey;

import javax.crypto.SecretKey;
import javax.json.*;

import java.io.StringReader;
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
    private Server serverToClient;
    private DirectoryController directoryController;

    public ServerController(Client clientToServer, Server serverToClient, DirectoryController directoryController, KeyService keyService) {
        this.clientToServer = clientToServer;
        this.serverToClient = serverToClient;
        this.directoryController = directoryController;
    }

    public boolean sync() {
        this.login(Crypto.secretKeyFromBytes("temp".getBytes())); // TODO

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
            JsonObject directoryKeychainObject = getDirectoryKeychainObject(serverid);

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
                    return false;
                }
            }
        }

        return true;
    }
    
    /**
     * Sends request to server and wait until response is received.
     * @param request
     * @return
     */
    private String sendRequestAndWait(String request){
    	int reqId = clientToServer.addRequest(request);
    	String response = null;
    	while(true){ // block until request is received
    		try {
				this.wait(2000);
				response = serverToClient.getResponse(reqId);
				if (response != null)
					break;
			} catch (InterruptedException e) {
				e.printStackTrace(); // should not happen in the current implementation
			}
    	}
    	
    	return response;
    }

    private JsonObject stringToJsonObject(String msg){
    	JsonReader jsonReader = Json.createReader(new StringReader(msg));
    	JsonObject object = jsonReader.readObject();
    	jsonReader.close();
    	return object;
    }
    
    private boolean login(SecretKey password) {
    	String req = RequestMessenger.createAuthenticateMessage(getUsername(), password.toString());
    	JsonObject res = stringToJsonObject(sendRequestAndWait(req));
    	return res.getString("status").equals("success");
    }

    private String getUsername() {
        return directoryController.getAccount().getUsername();
    }

    private List<Long> getKeychains() {
        String req = RequestMessenger.createGetKeychainsMessage(getUsername());
        JsonObject res = stringToJsonObject(sendRequestAndWait(req));
        JsonArray list = res.getJsonArray("keychains");
        List<Long> keys = new ArrayList<Long>();
        
        for (int i = 0; i < list.size(); i++){
        	JsonObject key = list.getJsonObject(i);
        	if (key.containsKey("directoryEntryId"))
        		keys.add((long) key.getInt("directoryEntryId"));
        }
        return keys;
    }

    private List<Long> getDeletedKeychains() {
        // Make request to server for list of deleted keychain's serverids
        return null;
    }

    private JsonObject getDirectoryKeychainObject(long serverid) {
        String req = RequestMessenger.createGetKeychainMessage(getUsername(), serverid);
        JsonObject res = stringToJsonObject(sendRequestAndWait(req));

        return res;
    }

    private boolean createKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        // TODO fill this in?
    	String req = RequestMessenger.createAddKeychainsMessage(getUsername(), entry.name, entry.encryptionKey.toString(), "", "");
    	JsonObject res = stringToJsonObject(sendRequestAndWait(req)); 
    	DirectoryAndKey dak = DirectoryAndKey.fromJsonObject(res.getJsonObject("keychain"));
    	long id = dak.getDirectoryEntryId();
    	
        // ask server for new keychain id;
        Optional<Long> newid = Optional.of(id);

        if (!newid.isPresent()) {
            LOGGER.warning("Failed to create keychain (" + entry.name + ")  on server...");
            return false;
        }

        directoryController.setKeychainOnlineId(entry, newid.get());

        return setKeychain(entry);
    }

    private boolean setKeychain(DirectoryEntry entry) {
        Optional<JsonObjectBuilder> keychainEntryObject = directoryController.buildKeychainEntryObject(entry);

        if (!keychainEntryObject.isPresent()) {
            return false;
        }

        // TODO fill this in?
        String req = RequestMessenger.createUpdateKeychainMessage(getUsername(), entry.name, entry.encryptionKey.toString(), "", "");
    	JsonObject res = stringToJsonObject(sendRequestAndWait(req)); 
    	return res.getString("status").equals("success");
    }

    private boolean deleteKeychain(long id) {
    	String req = RequestMessenger.createRemoveKeychainMessage(getUsername(), id);
    	JsonObject res = stringToJsonObject(sendRequestAndWait(req)); 
    	return res.getString("status").equals("success");
    }

    private Optional<Long> getNewKeychainId() {
        return Optional.empty();
    }
}
