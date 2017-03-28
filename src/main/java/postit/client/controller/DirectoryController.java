package postit.client.controller;

import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.keychain.*;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        if (keychain.passwords.stream().noneMatch(p -> p.identifier.equals(identifier))) {
            Password password = new Password(identifier, key, keychain);
            return keychain.passwords.add(password) && store.save();
        } else {
            return false;
        }
    }

    public boolean updatePassword(Password pass, SecretKey key) {
        pass.password = key;
        System.out.println("edited pass to: " + pass.dump().build());
        return store.save();
    }

    public boolean updatePasswordTitle(Password pass, String title) {
        if (pass.keychain.passwords.stream().noneMatch(p -> p.identifier.equals(title))) {
            pass.identifier = title;
            return store.save();
        } else {
            return false;
        }
    }

    public boolean updateMetadataEntry(Password password, String name, String entry) {
        password.metadata.put(name, entry);
        return store.save();
    }

    public boolean renameKeychain(Keychain keychain, String name) {
        Optional<DirectoryEntry> directoryEntry = this.directory.getKeychains().stream()
                .filter(entry -> entry.name.equals(keychain.getName()))
                .findAny();
        if (directoryEntry.isPresent()) {
            directoryEntry.get().setName(name);
            return store.save();
        }
        return false;
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

        boolean localIsOlder = entry.lastModified.isBefore(lastModified);
        if (localIsOlder) {
            entry.updateFrom(entryObject);
        }

        Optional<Keychain> keychain = entry.readKeychain();
        if (keychain.isPresent()) {
            // TODO replace this later
            JsonArray passwordArray = keychainObject.getJsonArray("passwords");
            Map<String, Password> passwords = new HashMap<>();
            Set<String> passwordIdentifiers = keychain.get().passwords.stream()
                    .map(password -> password.identifier)
                    .collect(Collectors.toSet());

            for (int i = 0; i < passwordArray.size(); i++) {
                JsonObject jsonPassword = passwordArray.getJsonObject(i);
                Password password = new Password(jsonPassword, keychain.get());
                if (!passwordIdentifiers.contains(password.identifier) || localIsOlder) {
                        passwords.put(password.identifier, password);
                }
            }

            for (Password password : keychain.get().passwords) {
                if (!passwords.containsKey(password.identifier)) {
                    passwords.put(password.identifier, password);
                }
            }

            keychain.get().initFrom(new ArrayList<>(passwords.values()));
        } else {
            LOGGER.warning("Failed to update entry " + entry.name + "from object (couldn't load keychain).");
            return false;
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

    public Optional<JsonObject> buildKeychainEntryObject(DirectoryEntry entry) {
        Optional<Keychain> keychain = entry.readKeychain();

        if (!keychain.isPresent()) {
            LOGGER.warning("Could not retrieve keychain " + entry.name + " from disk.");
            return Optional.empty();
        }

        return new DirectoryKeychain(keychain.get().dump().build(), entry.dump().build())
                .dump(getAccount().getKeyPair().getPublic());
    }

    public boolean setKeychainOnlineId(DirectoryEntry entry, long id) {
        entry.serverid = id;
        return store.save();
    }
}
