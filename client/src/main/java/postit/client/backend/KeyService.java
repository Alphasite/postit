package postit.client.backend;

import postit.client.keychain.Account;

import javax.crypto.SecretKey;

/**
 * Created by nishadmathur on 27/2/17.
 */
public interface KeyService {

    byte[] getKey(String keyName, Boolean isBeingCreated);

    SecretKey createMasterKey();

    SecretKey getMasterKey(Boolean isBeingCreated);
    
    SecretKey updateMasterKey();

    SecretKey getClientKey();

    Account getAccount();

    void destroyKey();
}
