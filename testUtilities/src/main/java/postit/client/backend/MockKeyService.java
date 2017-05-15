package postit.client.backend;

import postit.client.keychain.Account;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nishadmathur on 2/3/17.
 */
public class MockKeyService implements KeyService {
    public List<byte[]> getKeys;
    public List<SecretKey> clientKeys;

    public SecretKey masterKey;
    public SecretKey newMasterKey;

    public Account account;

    public MockKeyService(SecretKey masterKey, SecretKey newMasterKey) {
        this.getKeys = new ArrayList<>();
        this.clientKeys = new ArrayList<>();
        this.masterKey = masterKey;
        this.newMasterKey = newMasterKey;
    }

    @Override
    public byte[] getKey(String keyName,Boolean isBeingCreated) {
        return getKeys.remove(0);
    }

    @Override
    public SecretKey createMasterKey() {
        return newMasterKey;
    }

    @Override
    public SecretKey getMasterKey(Boolean isBeingCreated) {
        return masterKey;
    }

    @Override
    public SecretKey updateMasterKey() {
        this.masterKey = newMasterKey;
        return this.newMasterKey;
    }

    @Override
    public SecretKey getClientKey() {
        return clientKeys.remove(0);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void destroyKey() {
        // TODO?
    }
}
