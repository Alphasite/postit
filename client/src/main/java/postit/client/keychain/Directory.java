package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.passwordtools.PasswordGenerator;

import javax.crypto.SecretKey;
import javax.json.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class Directory {
    private final static Logger LOGGER = Logger.getLogger(Directory.class.getName());

    public static final String VERSION = "version";
    public static final String ACCOUNT = "account";
    public static final String KEYCHAINS = "keychains";
    public static final String DELETED = "deleted";
    public static final String PASSWORD_GENERATOR = "password-generator";

    private BackingStore backingStore;

    public List<DirectoryEntry> keychains;
    public List<Long> deletedKeychains;

    private Account account;

    private PasswordGenerator passwordGenerator;

    public Directory(BackingStore backingStore, KeyService keyService) {
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();
        this.account = keyService.getAccount();
        this.passwordGenerator = new PasswordGenerator();
    }

    public Directory(JsonObject object, BackingStore backingStore) {
        this.backingStore = backingStore;
        this.keychains = new ArrayList<>();
        this.deletedKeychains = new ArrayList<>();
        this.account = new Account(object.getJsonObject(ACCOUNT));
        this.passwordGenerator = new PasswordGenerator(object.getJsonObject(PASSWORD_GENERATOR));

        JsonArray keychainArray = object.getJsonArray(KEYCHAINS);
        for (int i = 0; i < keychainArray.size(); i++) {
            keychains.add(new DirectoryEntry(
                    account.getUsername(),
                    keychainArray.getJsonObject(i),
                    this,
                    backingStore
            ));
        }

        JsonArray deletedKeychainsArray = object.getJsonArray(DELETED);
        for (int i = 0; i < deletedKeychainsArray.size(); i++) {
            deletedKeychains.add(deletedKeychainsArray.getJsonNumber(i).longValue());
        }
    }

    public JsonObjectBuilder dump() {
        JsonArrayBuilder keychainArray = Json.createArrayBuilder();
        for (DirectoryEntry keychain : keychains) {
            keychainArray.add(keychain.dump());
        }

        JsonArrayBuilder deletedKeychainsArray = Json.createArrayBuilder();
        for (Long deletedKeychain : deletedKeychains) {
            deletedKeychainsArray.add(deletedKeychain);
        }

        return Json.createObjectBuilder()
                .add(VERSION, "1.0.0")
                .add(ACCOUNT, account.dump())
                .add(KEYCHAINS, keychainArray)
                .add(DELETED, deletedKeychainsArray)
                .add(PASSWORD_GENERATOR, passwordGenerator.dump());
    }

    public List<DirectoryEntry> getKeychains() {
        return keychains;
    }

    public boolean createKeychain(SecretKey encryptionKey, String name) {
        LOGGER.info("Creating keychain: " + name);

        DirectoryEntry entry = new DirectoryEntry(
                name,
                encryptionKey,
                this,
                backingStore,
                (RSAPublicKey) account.getEncryptionKeypair().getPublic(),
                (RSAPublicKey) account.getSigningKeypair().getPublic()
        );

        entry.setOwner(account.getUsername());

        if (keychains.stream().map(k -> k.name).anyMatch(n -> n.equals(name))) {
            LOGGER.warning("Keychain " + name +  "is a duplicate, not adding.");
            return false;
        }

        this.keychains.add(entry);

        return true;
    }

    public DirectoryEntry createKeychain(JsonObject entryObject, JsonObject keychainObject) {
        DirectoryEntry entry = new DirectoryEntry(account.getUsername(),  entryObject, this, backingStore);
        entry.keychain = new Keychain(keychainObject, entry);
        this.keychains.add(entry);

        return entry;
    }

    public boolean delete(DirectoryEntry keychain) {
        if (keychain.getServerid() != -1L) {
            deletedKeychains.add(keychain.getServerid());
        }

        this.keychains.remove(keychain);
        return this.backingStore.deleteKeychain(keychain.name);
    }

    public Optional<Account> getAccount() {
        return Optional.of(account);
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
