package keychain;

import backend.Crypto;
import backend.keyService.KeyService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    private final static Logger LOGGER = Logger.getLogger(Crypto.class.getName());

    String name;
    Path path;
    SecretKey encryptionKey;
    byte[] nonce;

    Keychain keychain;

    public DirectoryEntry(String name, Path path, SecretKey encryptionKey) {
        this.name = name;
        this.path = path;
        this.encryptionKey = encryptionKey;
        this.keychain = null;
    }

    public DirectoryEntry(JsonObject object, Path basePath) {
        this.name = object.getString("name");
        this.path = basePath.resolve(object.getString("path"));
        this.encryptionKey = Crypto.secretKeyFromBytes(object.getString("encryption-key").getBytes());
        this.keychain = null;
    }

    public JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("path", path.getFileName().toString());
        builder.add("encryption-key", new String(Crypto.secretKeyToBytes(encryptionKey)));

        return builder;
    }

    public Optional<Keychain> readKeychain(KeyService keyService) {
        Optional<Cipher> cipher = Crypto.getGCMDecryptCipher(keyService.getKey(), nonce);

        if (!cipher.isPresent()) {
            LOGGER.warning("Invalid password entered, unable to initialise encryption key.");
            return Optional.empty();
        }

        Optional<JsonObject> jsonObject = Crypto.readJsonObjectFromCipherStream(path, cipher.get());

        if (jsonObject.isPresent()) {
            return Optional.of(new Keychain(jsonObject.get()));
        } else {
            return Optional.empty();
        }
    }

    public boolean writeKeychain(KeyService keyService) {
        nonce = Crypto.getNonce();
        Optional<Cipher> cipher = Crypto.getGCMEncryptCipher(keyService.getKey(), nonce);

        if (!cipher.isPresent()) {
            return false;
        }

        return Crypto.writeJsonObjectToCipherStream(cipher.get(), path, keychain.dump().build());
    }


}
