package Keychain;

import Backend.Crypto;
import java.lang.UnsupportedOperationException;

import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.nio.file.Path;

/**
 * Created by nishadmathur on 23/2/17.
 */
public class DirectoryEntry {
    String name;
    Path path;
    SecretKey encryptionKey;

    public DirectoryEntry(String name, Path path, SecretKey encryptionKey) {
        this.name = name;
        this.path = path;
        this.encryptionKey = encryptionKey;
    }

    public DirectoryEntry(JsonObject object, Path basePath) {
        this.name = object.getString("name");
        this.path = basePath.resolve(object.getString("path"));
        this.encryptionKey = Crypto.secretKeyFromBytes(object.getString("encryption-key").getBytes());
    }

    protected JsonObjectBuilder dump() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("name", name);
        builder.add("path", path.getFileName().toString());
        builder.add("encryption-key", new String(Crypto.secretKeyToBytes(encryptionKey)));

        return builder;
    }

    public Keychain resolve() {
        throw new UnsupportedOperationException();
    }
}
