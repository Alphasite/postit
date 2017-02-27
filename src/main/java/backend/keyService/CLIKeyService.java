package backend.keyService;

import backend.Crypto;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.io.Console;
import java.time.*;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class CLIKeyService implements KeyService {
    static final Duration timeout = Duration.ofMinutes(15);

    private SecretKey key;
    private Instant retrieved;

    @Override
    public SecretKey getKey() {
        if (key == null || retrieved == null || Instant.now().isBefore(retrieved.plus(CLIKeyService.timeout))) {

            try {
                if (key != null) {
                    // Try destroy, but its not significant if it fails.
                    key.destroy();
                }
            } catch (DestroyFailedException ignored) {

            } finally {
                key = null;
            }


            Console console = System.console();
            String password = new String(console.readPassword("Please enter master password: "));
            key = Crypto.secretKeyFromBytes(password.getBytes());
        }

        retrieved = Instant.now();
        return key;
    }

    @Override
    public SecretKey createKey() {
        Console console = System.console();
        String password;

        while (true) {
            String password1 = new String(console.readPassword("Please enter NEW master password: "));
            String password2 = new String(console.readPassword("Please re-enter NEW master password: "));
            if (password1.equals(password2)) {
                password = password1;
                break;
            }
        }

        return Crypto.secretKeyFromBytes(password.getBytes());
    }

}
