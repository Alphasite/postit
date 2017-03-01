package gui;

import backend.KeyService;

import javax.crypto.SecretKey;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class GUIKeyService implements KeyService {
    @Override
    public byte[] getKey(String keyName) {
        return null;
    }

    @Override
    public SecretKey createKey() {
        return null;
    }

    @Override
    public SecretKey getMasterKey() {
        return null;
    }

    @Override
    public SecretKey getClientKey() {
        return null;
    }
}
