package postit.client.gui;

import postit.client.backend.KeyService;
import postit.client.controller.ServerController;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.swing.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class GUIKeyService implements KeyService {
    static final Duration timeout = Duration.ofMinutes(15);

    private SecretKey key;
    private Instant retrieved;

    private ServerController sc;

    GUIKeyService(){
    }

    GUIKeyService(ServerController sc){
        super();
        this.sc=sc;
    }

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

            key = null;

            while (key == null) {
                key = Crypto.secretKeyFromBytes(getKey("Please enter master password: "));
            }
        }

        retrieved = Instant.now();
        return key;
    }

    @Override
    public SecretKey getClientKey() {
        SecretKey key = null;
        while (key == null)
            key = Crypto.secretKeyFromBytes(getKey("Please enter client password: "));
        return key;

    }

    @Override
    public String getAccount() {
        String user = null;
        LoginPanel lp = new LoginPanel();
        while (user == null) {
            int result = JOptionPane.showConfirmDialog(null, lp,
                    "Login/Registration", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                if(lp.tabbedPane.getSelectedIndex()==0) {
                    // LOGIN
                    String username = lp.l_accountfield.getText();
                    String password = String.valueOf(lp.l_passfield.getPassword());
                    //if (sc.authenticate(username, Crypto.secretKeyFromBytes(password.getBytes()))) {
                    if (sc.authenticate(username, password)) {
                        user = lp.l_accountfield.getText();
                    }
                    else{
                        JOptionPane.showMessageDialog(null,"Login credentials invalid");
                    }
                }
                else if (lp.tabbedPane.getSelectedIndex()==1){
                    // REGISTRATION
                    String first = lp.r_firstfield.getText();
                    String last = lp.r_lastfield.getText();
                    String username = lp.r_accountfield.getText();
                    String pass1 = String.valueOf(lp.r_pass1field.getPassword());
                    String pass2 = String.valueOf(lp.r_pass2field.getPassword());
                    String email = lp.r_emailfield.getText();

                    if (pass1.equals(pass2) && LoginPanel.isValidEmailAddress(email)) {
                        if(sc.addUser(username, pass1,email,first,last)){
                            user=username;
                        }
                    }
                    else if (!pass1.equals(pass2)){
                        JOptionPane.showMessageDialog(null,"Passwords do not match");
                    }
                    else{
                        JOptionPane.showMessageDialog(null,"Email is invalid");
                    }
                }

            }
//            user = JOptionPane.showInputDialog("Please enter username: ");
        }

        return user;
    }
}
