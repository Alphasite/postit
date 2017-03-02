package backend;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by nishadmathur on 27/2/17.
 */
public interface KeyService {
    public byte[] getKey(String keyName);

    public SecretKey createMasterKey();

    public SecretKey getMasterKey();
    public SecretKey getClientKey();
}
