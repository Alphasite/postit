package postit.cli;

import postit.backend.Crypto;
import postit.backend.KeyService;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.io.Console;
import java.time.*;
import java.util.Scanner;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class CLIKeyService implements KeyService {
    static final Duration timeout = Duration.ofMinutes(15);

    private SecretKey key;
    private Instant retrieved;

    @Override
    public byte[] getKey(String message) {
        Console console = System.console();

        String password;
        if (console != null) {
            password = new String(console.readPassword(message));
        } else {
            System.err.println("No console detected. Ingesting text via StdIn"); // TODO log.
            System.out.println(message);
            password = new Scanner(System.in).nextLine();
        }

        return password.getBytes();
    }

    @Override
    public SecretKey getMasterKey() {
        if (key == null || retrieved == null || Instant.now().isAfter(retrieved.plus(CLIKeyService.timeout))) {

            try {
                if (key != null) {
                    // Try destroy, but its not significant if it fails.
                    key.destroy();
                }
            } catch (DestroyFailedException ignored) {

            } finally {
                key = null;
            }


            key = Crypto.hashedSecretKeyFromBytes(getKey("Please enter master password: "));
        }

        retrieved = Instant.now();
        return key;
    }

    @Override
    public SecretKey getClientKey() {
        return Crypto.secretKeyFromBytes(getKey("Please enter client password: "));
    }

    @Override
    public SecretKey createMasterKey() {
        String password;

        while (true) {
            String password1 = new String(getKey("Please enter NEW master password: "));
            String password2 = new String(getKey("Please re-enter NEW master password: "));
            if (password1.equals(password2)) {
                password = password1;
                break;
            }
        }

        key = Crypto.hashedSecretKeyFromBytes(password.getBytes());
        retrieved = Instant.now();

        return key;
    }
}
