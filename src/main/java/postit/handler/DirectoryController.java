package postit.handler;

import postit.backend.Crypto;
import postit.backend.KeyService;
import postit.keychain.Directory;
import postit.keychain.DirectoryEntry;
import postit.keychain.Keychain;
import postit.keychain.Password;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Optional;

/**
 * Created by jackielaw on 3/1/17.
 */
public class DirectoryController {
    private Directory directory;
    private KeyService keyService;

    public DirectoryController(Directory directory, KeyService keyService){
        //I'm not 100% if we want to do it this way, so feel free to change the constructor params
        this.directory = directory;
        this.keyService = keyService;
    }

    public List<DirectoryEntry> getKeychains(){
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

    public List<Password> getPasswords(Keychain k){
        return k.passwords;
    }

    public boolean createKeychain(String keychainName){
        return directory.createKeychain(keyService.getMasterKey(), keychainName).isPresent() && directory.save();
    }
    public boolean createPassword(Keychain keychain, String identifier, SecretKey key) {
        Password password = new Password(identifier, key, keychain);
        return keychain.passwords.add(password) && password.save();
    }

    public boolean updatePassword(Password pass, SecretKey key){
        pass.password = key;
        return pass.save();
    }

    public boolean deleteKeychain(Keychain k){
        return k.delete();
    }
    public boolean deletePassword(Password p){
        return p.delete();
    }

    public String getPassword(Password p) {
        return new String(Crypto.secretKeyToBytes(p.password));
    }
}