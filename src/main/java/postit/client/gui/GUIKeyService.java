package postit.client.gui;

import postit.client.keychain.Account;
import postit.client.keychain.Directory;
import postit.shared.Crypto;
import postit.client.backend.KeyService;

import javax.swing.*;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class GUIKeyService implements KeyService {
    static final Duration timeout = Duration.ofMinutes(15);

    private SecretKey key;
    private Instant retrieved;

    @Override
    public byte[] getKey(String displayMessage) {
        return JOptionPane.showInputDialog(displayMessage).getBytes();
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

        key = Crypto.secretKeyFromBytes(password.getBytes());
        retrieved = Instant.now();

        return key;
    }

    @Override
    public SecretKey getMasterKey() {
        if (key == null || retrieved == null || Instant.now().isAfter(retrieved.plus(GUIKeyService.timeout))) {

            try {
                if (key != null) {
                    // Try destroy, but its not significant if it fails.
                    key.destroy();
                }
            } catch (DestroyFailedException ignored) {

            } finally {
                key = null;
            }

            key = Crypto.secretKeyFromBytes(getKey("Please enter master password: "));
        }

        retrieved = Instant.now();
        return key;
    }

    @Override
    public SecretKey getClientKey() {
        return Crypto.secretKeyFromBytes(getKey("Please enter client password: "));

    }

    @Override
    public String getAccount() {
        return JOptionPane.showInputDialog("Please enter username: ");
    }
}
