package postit.client.controller;

import postit.client.backend.BackingStore;
import postit.client.keychain.*;
import postit.shared.Crypto;
import postit.client.backend.KeyService;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DirectoryController {
    private final static Logger LOGGER = Logger.getLogger(DirectoryController.class.getName());

    private BackingStore store;
    private Directory directory;
    private KeyService keyService;

    public DirectoryController(Directory directory, BackingStore store, KeyService keyService) {
        //I'm not 100% if we want to do it this way, so feel free to change the constructor params
        this.directory = directory;
        this.store = store;
        this.keyService = keyService;
    }

    public List<DirectoryEntry> getKeychains() {
        return directory.getKeychains();
    }

    public Optional<Keychain> getKeychain(String name) {
        Optional<DirectoryEntry> directoryEntry = this.directory.getKeychains().stream()
                .filter(entry -> entry.name.equals(name))
                .findAny();

        if (directoryEntry.isPresent()) {
            return directoryEntry.get().readKeychain();
        } else {
            return Optional.empty();
        }
    }

    public Optional<Keychain> getKeychain(DirectoryEntry directoryEntry) {
        return directoryEntry.readKeychain();
    }

    public List<Password> getPasswords(Keychain k) {
        return k.passwords;
    }

    public boolean createKeychain(String keychainName) {
        return directory.createKeychain(keyService.getMasterKey(), keychainName) && store.save();
    }

    public boolean createPassword(Keychain keychain, String identifier, SecretKey key) {
        Password password = new Password(identifier, key, keychain);
        return keychain.passwords.add(password) && store.save();
    }

    public boolean updatePassword(Password pass, SecretKey key) {
        pass.password = key;
        System.out.println("edited pass to: " + pass.dump().build());
        return store.save();
    }

    public boolean updatePasswordTitle(Password pass, String title) {
        pass.identifier = title;
        return store.save();
    }

    public boolean updateMetadataEntry(Password password, String name, String entry) {
        password.metadata.put(name, entry);
        return store.save();
    }

    public boolean removeMetadataEntryIfExists(Password password, String name) {
        password.metadata.remove(name);
        return store.save();
    }

    public boolean deleteKeychain(Keychain k) {
        return k.delete() && store.save();
    }

    public boolean deleteEntry(DirectoryEntry entry) {
        return entry.delete() && store.save();
    }

    public boolean deletePassword(Password p) {
        return p.delete() && store.save();
    }

    public String getPassword(Password p) {
        return new String(Crypto.secretKeyToBytes(p.password));
    }

    public List<Long> getDeletedKeychains() {
        return directory.deletedKeychains;
    }

    public boolean updateLocalIfIsOlder(DirectoryEntry entry, JsonObject entryObject, JsonObject keychainObject) {
        LocalDateTime lastModified = LocalDateTime.parse(entryObject.getString("lastModified"));

        // TODO make better (e.g. handle simultaneous edits)
        // merge

        if (entry.lastModified.isBefore(lastModified)) {
            entry.updateFrom(entryObject);

            Optional<Keychain> keychain = entry.readKeychain();
            if (keychain.isPresent()) {
                // TODO replace this later
                keychain.get().initFrom(keychainObject);
            } else {
                LOGGER.warning("Failed to update entry " + entry.name + "from object (couldn't load keychain).");
                return false;
            }
        }

        return store.save();
    }

    public boolean createKeychain(JsonObject directory, JsonObject keychain) {
        this.directory.createKeychain(directory, keychain);
        return store.save();
    }

    public Account getAccount() {
        return this.directory.account;
    }

    public Optional<JsonObjectBuilder> buildKeychainEntryObject(DirectoryEntry entry) {
        // TODO encryption.
        Optional<Keychain> keychain = entry.readKeychain();

        if (!keychain.isPresent()) {
            LOGGER.warning("Could not retrieve keychain " + entry.name + " from disk.");
            return Optional.empty();
        }

        return Optional.of(
                Json.createObjectBuilder()
                        .add("directory", entry.dump())
                        .add("keychain", keychain.get().dump())
                        .add("lastModified", entry.lastModified.toString())
        );
    }

    public boolean setKeychainOnlineId(DirectoryEntry entry, long id) {
        entry.serverid = id;
        return store.save();
    }
}
