package backend;

import javax.crypto.SecretKey;

/**
 * Created by nishadmathur on 27/2/17.
 */
public interface KeyService {
    public SecretKey getKey();
    public SecretKey createKey();
}
