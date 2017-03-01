package cli;

import backend.Crypto;
import backend.KeyService;

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

            String password;
            if (console != null) {
                password = new String(console.readPassword("Please enter master password: "));
            } else {
                System.err.println("No console detected. Ingesting text via StdIn"); // TODO log.
                System.out.println("Please enter master password: ");
                password = new Scanner(System.in).nextLine();
            }

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
