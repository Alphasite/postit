package keychain;

import backend.BackingStore;
import backend.Crypto;
import backend.KeyService;
import com.sun.xml.internal.rngom.parse.host.Base;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(DirectoryEntry.class.getName());

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
        this.nonce = Base64.getDecoder().decode(object.getString("nonce"));
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("encryption-key", new String(Crypto.secretKeyToBytes(encryptionKey)));
        builder.add("nonce", Base64.getEncoder().encodeToString(nonce));

        return builder;
    }

    public Optional<Keychain> readKeychain() {
        if (!Files.exists(getPath())) {
            keychain = new Keychain(name, this);
            this.save();

            return Optional.of(keychain);
        }

        Optional<Cipher> cipher = Crypto.getGCMDecryptCipher(encryptionKey, nonce);

        if (!cipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromCipherStream(getPath(), cipher.get());

        if (jsonObject.isPresent()) {
            keychain = new Keychain(jsonObject.get(), this);
            return Optional.of(keychain);
        } else {
            return Optional.empty();
        }
    }

    public boolean save() {
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
