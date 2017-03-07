package postit.client.keychain;

import postit.client.backend.BackingStore;
import postit.client.controller.DirectoryController;
import postit.shared.Crypto;
import postit.client.backend.KeyService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(DirectoryEntry.class.getName());

    public String name;
    public Long serverid;

    public SecretKey encryptionKey;
    private byte[] nonce;

    Directory directory;
    Keychain keychain;

    KeyService keyService;
    BackingStore backingStore;

    public LocalDateTime lastModified;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.name = name;
        this.encryptionKey = encryptionKey;
        this.directory = directory;
        this.keychain = null;
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.lastModified = LocalDateTime.now();
    }

    public DirectoryEntry(JsonObject object, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.directory = directory;
        this.keychain = null;
        this.keyService = keyService;
        this.backingStore = backingStore;
        this.updateFrom(object);
    }

    public void updateFrom(JsonObject object) {
        this.name = object.getString("name");
        this.encryptionKey = Crypto.secretKeyFromBytes(Base64.getDecoder().decode(object.getString("encryption-key").getBytes()));
        this.nonce = Base64.getDecoder().decode(object.getString("nonce"));
        this.serverid = object.getJsonNumber("serverid").longValue();
        this.lastModified = LocalDateTime.parse(object.getString("lastModified"));
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("encryption-key", new String(Base64.getEncoder().encode(Crypto.secretKeyToBytes(encryptionKey))));
        builder.add("nonce", Base64.getEncoder().encodeToString(nonce));
        builder.add("serverid", serverid);
        builder.add("lastModified", lastModified.toString()); // TODO check this handles timezones correctly

        return builder;
    }

    public Optional<Keychain> readKeychain() {
        LOGGER.info("Reading keychain " + name);

        if (!Files.exists(getPath())) {
            LOGGER.info("Keychain file doesn't exist... creating it: " + name);

            keychain = new Keychain(name, this);
            this.save();

            return Optional.of(keychain);
        }

        if (keychain != null) {
            return Optional.of(keychain);
        }

        Optional<Cipher> cipher = Crypto.getGCMDecryptCipher(encryptionKey, nonce);

        if (!cipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromCipherStream(getPath(), cipher.get());

        if (jsonObject.isPresent()) {
            LOGGER.info("Succeeded in reading keychain " + name + " from disk.");
            keychain = new Keychain(jsonObject.get(), this);
            return Optional.of(keychain);
        } else {
            return Optional.empty();
        }
    }

    public boolean save() {
        LOGGER.info("Saving Directory Entry " + name);

        if (keychain == null) {
            return this.readKeychain().isPresent();
        }

        nonce = Crypto.getNonce();

        SecretKey newKey = Crypto.generateKey();
        SecretKey oldKey = encryptionKey;

        Optional<Cipher> cipher = Crypto.getGCMEncryptCipher(newKey, nonce);

        if (!cipher.isPresent()) {
            return false;
        }

        Path path = getPath();
        boolean success = Crypto.writeJsonObjectToCipherStream(cipher.get(), path, keychain.dump().build());
        System.out.println("Saved keychain as: " + keychain.dump().build());

        if (success) {
            encryptionKey = newKey;

            if (!directory.save()) {
                encryptionKey = oldKey;

                LOGGER.severe("Failed to save keychain.");

                success = false;
            }
        }

        if (!success) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                LOGGER.severe("Failed to delete orphan keychain: " + e.getMessage());
            }

            return false;
        }

        LOGGER.info("Succeeded in writing keychain " + name + " to disk.");
        return true;
    }

    public boolean delete() {
        try {
            directory.keychains.remove(this);
            if (directory.save()) {
                Files.delete(getPath());
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            LOGGER.severe("Failed while deleting keychain " + name + ": " + e.getMessage());
            return false;
        }
    }

    public Path getPath() {
        return backingStore.getKeychainsPath().resolve(name + ".keychain");
    }
}
