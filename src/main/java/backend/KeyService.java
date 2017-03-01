package backend;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by nishadmathur on 27/2/17.
 */
public interface KeyService {
    public byte[] getKey(String keyName);
    public SecretKey createKey();

    public SecretKey getMasterKey();

    default SecretKey getClientKey() {
        return new SecretKeySpec(getKey("client"), "RAW");
    }
}
