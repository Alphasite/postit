package postit.client.gui;

import org.json.JSONObject;
import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.controller.ServerController;
import postit.client.keychain.Account;
import postit.client.passwordtools.Classify;
import postit.shared.Crypto;
import postit.shared.EFactorAuth;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.swing.*;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Created by nishadmathur on 27/2/17.
 */
public class GUIKeyService implements KeyService {
    static final Duration timeout = Duration.ofMinutes(15);

    private SecretKey key;
    private Instant retrieved;

    private ServerController sc;
    private BackingStore backingStore;

    public GUIKeyService(ServerController sc) {
        this.sc = sc;
    }

    public void setBackingStore(BackingStore backingStore) {
        this.backingStore = backingStore;
    }

    @Override
    public byte[] getKey(String displayMessage) {
        String key = null;
        Classify classify = new Classify();
        boolean strong = false;
        do {
            while (key == null) {
                key = JOptionPane.showInputDialog(null, displayMessage, "", JOptionPane.PLAIN_MESSAGE);
                strong = !classify.isWeak(key);
            }
            if (!strong){
                JOptionPane.showMessageDialog(null,"Master password is too weak");
                key=null;
            }
        }while (!strong);
        return key.getBytes();
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
    public SecretKey updateMasterKey(){
        String password;

        while (true) {
        	String passwordOld1 = new String(getMasterKey().getEncoded());
        	String passwordOld2 = new String(getKey("Current master password"));
        	if (! passwordOld1.equals(passwordOld2)){
        		JOptionPane.showMessageDialog(null, "The CURRENT master password is incorrect.");
        		return null;
        	}
            String password1 = new String(getKey("New master password"));
            String password2 = new String(getKey("Re-enter new master password"));
            if (password1.equals(password2)) {
                password = password1;
                break;
            }
        }

    	backingStore.readDirectory();
        
        key = Crypto.secretKeyFromBytes(password.getBytes());
        retrieved = Instant.now();
        
        backingStore.writeDirectory();
        
        if (!backingStore.saveContainer()){
        	return null;
        }

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
    public Account getAccount() {
        LoginPanel lp = new LoginPanel();

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, lp,
                    "Login/Registration", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                if (lp.tabbedPane.getSelectedIndex() == 0) {
                    // LOGIN
                    String username = lp.l_accountfield.getText();
                    String password = String.valueOf(lp.l_passfield.getPassword());

                    Account newAccount = new Account(username, password);
                    //authenticate via password
                    if (sc.authenticate(newAccount)) {
                        //authenticate via text
                        //TODO send text
                        String phoneNumber="6073794979";
                        new EFactorAuth().sendMsg(phoneNumber);
                        String pin = null;
                        while (pin==null){
                            pin = JOptionPane.showInputDialog("Enter PIN sent to your phone: ");
                        }
                        if(new EFactorAuth().verifyMsg(phoneNumber,pin)){
                            JOptionPane.showConfirmDialog(
                                    null,
                                    "Please ensure your keypair is in the data directory. Select any option to proceed"
                            );

                            Optional<KeyPair> keyPair = backingStore.readKeypair();
                            if (keyPair.isPresent()) {
                                newAccount.setKeyPair(keyPair.get());
                                return newAccount;
                            } else {
                                JOptionPane.showMessageDialog(null, "Failed to load keypair.");
                            }
                        }else {
                            JOptionPane.showMessageDialog(null, "Incorrect PIN");
                        }

                    } else {
                        JOptionPane.showMessageDialog(null, "Login credentials invalid");
                    }

                } else if (lp.tabbedPane.getSelectedIndex() == 1) {
                    // REGISTRATION
                    String first = lp.r_firstfield.getText();
                    String last = lp.r_lastfield.getText();
                    String username = lp.r_accountfield.getText();
                    String pass1 = String.valueOf(lp.r_pass1field.getPassword());
                    String pass2 = String.valueOf(lp.r_pass2field.getPassword());
                    String email = lp.r_emailfield.getText();
                    String phone = lp.r_phonefield.getText().replaceAll("[-()\\s]","");

                    Classify classify = new Classify();
                    JSONObject passresult =classify.strengthCheck(pass1);
                    String evaluation = (String) passresult.get("evaluation");
                    if (pass1.equals(pass2)
                            && !classify.isWeak(pass1)
                            && LoginPanel.isValidEmailAddress(email)
                            && LoginPanel.isValidPhoneNumber(phone)) {
                        Account newAccount = new Account(username, pass1);
                        if (sc.addUser(newAccount, email, first, last, phone)) {
                            if (backingStore.writeKeypair(newAccount.getKeyPair())) {
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Generated a new keypair and saved it to the disk. Please transfer this " +
                                            "to a memory stick and store it in a safe, or other secure location as you " +
                                            "will need it to login at new locations."
                                );

                                return newAccount;
                            } else {
                                JOptionPane.showMessageDialog(null, "Failed to save generated key pair.");
                            }
                        }
                    } else if (!pass1.equals(pass2)) {
                        JOptionPane.showMessageDialog(null, "Passwords do not match");
                    } else if (!LoginPanel.isValidEmailAddress(email)){
                        JOptionPane.showMessageDialog(null, "Email is invalid");
                    } else if(!LoginPanel.isValidPhoneNumber(phone)){
                        JOptionPane.showMessageDialog(null, "Phone number should be 10 digits only");
                    }else{
                        JOptionPane.showMessageDialog(null, "Password is too weak. \n Improvements: "+evaluation);
                    }
                }

            }
        }
    }
}
