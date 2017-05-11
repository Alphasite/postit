package postit.client.controller;

import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.keychain.*;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
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

    public boolean createPassword(Keychain keychain, String title, String username, SecretKey key) {
        Password password = new Password(UUID.randomUUID().toString(), key, keychain);
        password.metadata.put("username", username);
        password.metadata.put("title", title);
        password.markUpdated();
        return keychain.passwords.add(password) && store.save();
    }

    public boolean updatePassword(Password pass, SecretKey key) {
        pass.password = key;
        pass.markUpdated();
        return store.save();
    }

    public boolean updatePasswordTitle(Password pass, String title) {
        pass.metadata.put("title", title);
        pass.markUpdated();
        return store.save();
    }

    public boolean updateMetadataEntry(Password password, String name, String entry) {
        password.metadata.put(name, entry);
        password.markUpdated();
        return store.save();
    }

    public boolean renameKeychain(Keychain keychain, String name) {
        Optional<DirectoryEntry> directoryEntry = this.directory.getKeychains().stream()
                .filter(entry -> entry.name.equals(keychain.getName()))
                .findAny();
        if (directoryEntry.isPresent()) {
            directoryEntry.get().setName(name);
            directoryEntry.get().markUpdated();
            return store.save();
        }
        keychain.markUpdated();
        return false;
    }

    public boolean removeMetadataEntryIfExists(Password password, String name) {
        password.metadata.remove(name);
        password.markUpdated();
        return store.save();
    }

    public boolean deleteKeychain(Keychain k) {
        k.markUpdated();
        return k.delete() && store.save();
    }

    public boolean deleteEntry(DirectoryEntry entry) {
        entry.markUpdated();
        return entry.delete() && store.save();
    }

    public boolean deletePassword(Password p) {
        p.delete();
        return store.save();
    }

    public String getPassword(Password p) {
        return new String(Crypto.secretKeyToBytes(p.password));
    }

    public List<Long> getDeletedKeychains() {
        return directory.deletedKeychains;
    }

    public boolean updateLocalIfIsOlder(DirectoryEntry entry, JsonObject entryObject, JsonObject keychainObject, String entryOwnerUsername, boolean isOwnersEntry) {
        LocalDateTime lastModified = LocalDateTime.parse(entryObject.getString("lastModified"));

        // TODO make better (e.g. handle simultaneous edits)
        // merge

        boolean localIsOlder = entry.lastModified.isBefore(lastModified);
        if (localIsOlder) {
            entry.updateFrom(entryObject);
        }

        Optional<Keychain> keychain = entry.readKeychain();
        if (keychain.isPresent()) {
            Set<Long> shareIdentifiers = entry.shares.stream()
                    .map(share -> share.serverid)
                    .collect(Collectors.toSet());

            DirectoryEntry newEntry;

            try {
                newEntry = new DirectoryEntry(entryOwnerUsername, entryObject, null, null);
            } catch (NullPointerException e) {
                LOGGER.warning("Keychain doesnt have an entry for the user... aborting.");
                return false;
            }

            if (isOwnersEntry) {
                for (Share share : newEntry.shares) {
                    if (!shareIdentifiers.contains(share.serverid) || localIsOlder) {
                        Optional<Share> localShare = entry.shares.stream()
                                .filter(s -> s.serverid == share.serverid)
                                .findAny();

                        localShare.ifPresent(share1 -> entry.shares.remove(share1));
                        entry.shares.add(share);
                    }
                }
            }

            // TODO replace this later
            JsonArray passwordArray = keychainObject.getJsonArray("passwords");
            JsonArray deletedPasswordArray = keychainObject.getJsonArray("deleted-passwords");

            for (int i = 0; i < deletedPasswordArray.size(); i++) {
                keychain.get().deletedPasswords.add(deletedPasswordArray.getString(i));
            }

            HashMap<String, List<Password>> allPasswords = new HashMap<>();

            // Gather all local copies of passwords
            for (Password password : keychain.get().passwords) {
                List<Password> passwordList = allPasswords.getOrDefault(password.uuid, new ArrayList<>());
                passwordList.add(password);
                allPasswords.put(password.uuid, passwordList);
            }

            // Gather all remote copies of passwords
            for (int i = 0; i < passwordArray.size(); i++) {
                JsonObject jsonPassword = passwordArray.getJsonObject(i);
                Password password = new Password(jsonPassword, keychain.get());
                List<Password> passwordList = allPasswords.getOrDefault(password.uuid, new ArrayList<>());
                passwordList.add(password);
            }

            // Select the newest version and use that.
            ArrayList<Password> passwords = new ArrayList<>();
            for (List<Password> passwordList : allPasswords.values()) {
                if (passwordList.size() == 1) {
                    passwords.add(passwordList.get(0));
                } else {
                    Password password1 = passwordList.get(0);
                    Password password2 = passwordList.get(1);
                    if (password1.lastModified.isBefore(password2.lastModified)) {
                        passwords.add(password2);
                    } else {
                        passwords.add(password1);
                    }
                }
            }

            keychain.get().initFrom(passwords);
        } else {
            LOGGER.warning("Failed to update entry " + entry.name + "from object (couldn't load keychain).");
            return false;
        }

        return store.save();
    }

    public boolean createKeychain(long serverId, JsonObject directory, JsonObject keychain) {
        DirectoryEntry entry = this.directory.createKeychain(directory, keychain);
        entry.setServerid(serverId);
        return store.save();
    }

    public Optional<Account> getAccount() {
        return this.directory.getAccount();
    }

    public Optional<JsonObject> buildKeychainEntryObject(DirectoryEntry entry) {
        Optional<Keychain> keychain = entry.readKeychain();

        if (!keychain.isPresent()) {
            LOGGER.warning("Could not retrieve keychain " + entry.name + " from disk.");
            return Optional.empty();
        }

        Optional<Account> account = getAccount();
        if (!account.isPresent()) {
            LOGGER.warning("No account availible; cant build KDO");
            return Optional.empty();
        }

        return new DirectoryKeychain(entry.getServerid(), keychain.get().dump().build(), entry.dump().build())
                .dump(entry, account.get());
    }

    public boolean setKeychainOnlineId(DirectoryEntry entry, long id) {
        entry.setServerid(id);
        entry.markUpdated();
        return store.save();
    }

    public boolean setKeychainSharedId(DirectoryEntry entry, long id, Share share) {
        share.serverid = id;
        entry.markUpdated();
        return store.save();
    }

    public boolean shareKeychain(DirectoryEntry entry, Share share) {
        if (share.isOwner) {
            return false;
        } else {
            entry.shares.add(share);
            entry.markUpdated();
            return store.save();
        }
    }

    public boolean unshareKeychain(DirectoryEntry entry, Share share) {
        if (share.isOwner) {
            return false;
        } else {
            entry.shares.remove(share);
            entry.markUpdated();
            return store.save();
        }

    }

    public boolean setKeychainOwner(DirectoryEntry entry, String username) {
        entry.setOwner(username);
        entry.markUpdated();
        return store.save();
    }

    public List<Password> getExpired(Keychain keychain, TemporalAmount validityPeriod) {
        LocalDateTime expireyDate = LocalDateTime.now().minus(validityPeriod);
        return keychain.passwords.stream()
                .filter(password -> password.lastModified.isBefore(expireyDate))
                .collect(Collectors.toList());
    }

    public boolean save() {
        return store.save();
    }
}
