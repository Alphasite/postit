package postit.client.backend;

import jdk.internal.util.xml.impl.Pair;
import postit.client.keychain.Account;

import javax.crypto.SecretKey;

/**
 * Created by nishadmathur on 27/2/17.
 */
public interface KeyService {

    byte[] getKey(String keyName);

    SecretKey createMasterKey();

    SecretKey getMasterKey();

    SecretKey getClientKey();

    String getAccount();
}
