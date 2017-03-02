package handler;

import javax.crypto.SecretKey;

import keychain.Directory;
import keychain.Keychain;
import keychain.Password;

import java.util.List;

/**
 * Created by jackielaw on 3/1/17.
 */
public class DirectoryController {
    private Directory directory;

    public DirectoryController(){

    }

    public DirectoryController(Directory directory){
        //I'm not 100% if we want to do it this way, so feel free to change the constructor params
        this.directory = directory;
    }

    public List<Keychain> getKeychains(){
        return new List<Keychain>;
    }

    public List<Password> getPasswords(Keychain k){
        return new List<Password>;
    }

    public boolean createKeychain(String keychainName){
        return true;
    }
    public boolean createPassword(Keychain k, String Passwordname){
        return true;
    }

    public boolean updatePassword(Password pass, SecretKey key, Keychain k){
        return true;
    }

    public boolean deleteKeychain(Keychain k){
        return true;
    }
    public boolean deletePassword(Keychain k, Password p){
        return true;
    }
}
