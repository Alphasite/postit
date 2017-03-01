package keychain;

import backend.BackingStore;
import backend.Crypto;
import backend.KeyService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(Crypto.class.getName());

    public String name;
    public SecretKey encryptionKey;
    private byte[] nonce;

    Directory directory;
    Keychain keychain;

    KeyService keyService;
    BackingStore backingStore;

    public DirectoryEntry(String name, SecretKey encryptionKey, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.name = name;
        this.encryptionKey = encryptionKey;
        this.directory = directory;
        this.keyService = keyService;
        this.backingStore = backingStore;
    }

    public DirectoryEntry(JsonObject object, Directory directory, KeyService keyService, BackingStore backingStore) {
        this.name = object.getString("name");
        this.encryptionKey = Crypto.secretKeyFromBytes(object.getString("encryption-key").getBytes());
        this.directory = directory;
        this.keyService = keyService;
        this.backingStore = backingStore;
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("encryption-key", new String(Crypto.secretKeyToBytes(encryptionKey)));

        return builder;
    }

    public Optional<Keychain> readKeychain() {
        if (!Files.exists(getPath())) {
            keychain = new Keychain(name, this);
            this.save();

            return Optional.of(keychain);
        }

        Optional<Cipher> cipher = Crypto.getGCMDecryptCipher(keyService.getKey(), nonce);

        if (!cipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromCipherStream(getPath(), cipher.get());

        if (jsonObject.isPresent()) {
            return Optional.of(new Keychain(jsonObject.get(), this));
        } else {
            return Optional.empty();
        }
    }

    public boolean save() {
        if (keychain == null) {
            return this.readKeychain().isPresent();
        }

        nonce = Crypto.getNonce();
        Optional<Cipher> cipher = Crypto.getGCMEncryptCipher(keyService.getKey(), nonce);

        if (!cipher.isPresent()) {
            return false;
        }

        boolean success = Crypto.writeJsonObjectToCipherStream(cipher.get(), getPath(), keychain.dump().build());

        if (success) {
            if (!directory.save()) {
                LOGGER.severe("Failed to save keychain.");

                try {
                    Files.delete(getPath());
                } catch (IOException e) {
                    LOGGER.severe("Failed to delete orphan keychain: " + e.getMessage());
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean delete() {
        try {
            directory.keychains.remove(this);
            Files.delete(getPath());
            directory.delete(this);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed while deleting keychain " + name + ": " + e.getMessage());
            return false;
        }
    }

    public Path getPath() {
        return backingStore.getKeychainsPath().resolve(name + ".keychain");
    }
}
