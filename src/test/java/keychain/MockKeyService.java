package keychain;

import backend.KeyService;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nishadmathur on 2/3/17.
 */
public class MockKeyService implements KeyService {
    List<byte[]> getKeys;
    List<SecretKey> clientKeys;

    SecretKey masterKey;

    public MockKeyService(SecretKey masterKey) {
        this.getKeys = new ArrayList<>();
        this.clientKeys = new ArrayList<>();
        this.masterKey = masterKey;
    }

    @Override
    public byte[] getKey(String keyName) {
        return getKeys.remove(0);
    }

    @Override
    public SecretKey createMasterKey() {
        return masterKey;
    }

    @Override
    public SecretKey getMasterKey() {
        return masterKey;
    }

    @Override
    public SecretKey getClientKey() {
        return clientKeys.remove(0);
    }
}
