
package postit.client.gui;


import org.json.JSONObject;
import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.controller.DirectoryController;
import postit.client.controller.ServerController;
import postit.client.keychain.*;
import postit.client.log.KeychainLog;
import postit.client.passwordtools.Classify;
import postit.client.passwordtools.PasswordGenerator;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * Created by jackielaw on 2/27/17.
 */
public class KeychainViewer {
    KeychainViewer kv = this;
    DirectoryController directoryController;
    ServerController serverController;
    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane = new JTabbedPane();
    private List<DirectoryEntry> keychains;
    //each JTable contains a list of passwords in a particular Keychain
    private List<JTable> tables = new ArrayList<>();

    private Password selectedPassword;
    private List<JMenuItem> passwordMenuItems= new List<JMenuItem>();
    private List<JMenuItem> keychainMenuItems= new List<JMenuItem>();
    private JMenuItem addPass;
    private JMenuItem delPass;
    private JMenuItem movePass;
    private JMenuItem delKey;
    private JMenuItem rnKey;
    private JMenuItem addKeyPerm;
    private JMenuItem rmKeyPerm;
    private JMenuItem showKeyPerm;
    private JMenuItem showKeyLogs;

    private PasswordGenerator passwordGenerator;
    private Classify classify;
    private KeyService keyService;

    private KeychainLog keyLog;

    public KeychainViewer(ServerController serverController, BackingStore backingStore, KeyService keyService, KeychainLog keyLog) {

        this.serverController = serverController;

        Optional<Directory> directory = backingStore.readDirectory();


        if (!directory.isPresent()) {
            JOptionPane.showMessageDialog(null,
                    "Could not load directory. Master password may be wrong or data has been compromised");
        } else {
            directoryController = new DirectoryController(directory.get(), backingStore, keyService);
            this.keychains = directoryController.getKeychains();

            serverController.setDirectoryController(directoryController);
            createUIComponents();
        }

        passwordGenerator = new PasswordGenerator();
        classify = new Classify();
        this.keyService = keyService;
        
        this.keyLog = keyLog;
    }

    /**
     * creatUIComponents()
     * <p>
     * Creates the UI with each tabbedPane as a keychain
     * Each tabbedPane contains a table that holds all the passwords
     * for that keychain
     */
    private void createUIComponents() {
        JFrame frame = new JFrame("Keychain");
        frame.setLayout(new GridLayout());
        frame.setMinimumSize(new Dimension(520, 485));
        frame.setMaximumSize(new Dimension(520, 485));
        menuBar = new JMenuBar();

        //FILE Menu Items
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        addPass = new JMenuItem("New Password");
        passwordMenuItems.add(addPass);
        addPass.addActionListener(e -> {
            JTextField newtitle = new JTextField();
            JTextField newusername = new JTextField();
            JTextField newpassword = new JTextField();

            JButton generatePass = new JButton("Generate Password");
            JLabel passwordStrength = new JLabel("\n");
            newpassword.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    passwordStrength.setText("\n");
                }
                @Override
                public void focusLost(FocusEvent e) {
                    JSONObject result = classify.strengthCheck(newpassword.getText());
                    String strength = (String) result.get("strength");

                    passwordStrength.setText("Password Strength: "+strength);
                }
            });
            generatePass.addActionListener(ee->{newpassword.setText(passwordGenerator.generatePassword());});

            Object[] message = {
                    "Title:", newtitle,
                    "Username", newusername,
                    "Password:", newpassword,
                    generatePass,
                    passwordStrength
            };

            int option = JOptionPane.showConfirmDialog(frame, message, "New Password", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                if (newtitle.getText().length() > 0 && newpassword.getText().length() > 0
                        && newusername.getText().length() > 0) {
                    boolean success = directoryController.createPassword(getActiveKeychain(),
                            newtitle.getText(), newusername.getText(),
                            Crypto.secretKeyFromBytes(newpassword.getText().getBytes()));
                    if (success){
                    	Keychain key = getActiveKeychain();
                    	Optional<Account> act = directoryController.getAccount();
                    	if (act.isPresent()){
                    		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getName(), 
                    				String.format("Password %s added to keychain <%s>", newtitle.getText(), key.getName()));
                    	}
                    }
                    else{
                        JOptionPane.showMessageDialog(frame,"Unable to add " + newtitle.getText() +" password",
                                "Warning!",JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
            refreshTabbedPanes();
        });
        addPass.setEnabled(false);
        fileMenu.add(addPass);

        movePass = new JMenuItem("Move Password");
        passwordMenuItems.add(movePass);
        movePass.addActionListener(e -> {
            ArrayList<String> choicesList = new ArrayList<String>();
            for(DirectoryEntry de : keychains){
                choicesList.add(de.name);
            }
            String[] choices = choicesList.toArray(new String[choicesList.size()]);
            String input =(String) JOptionPane.showInputDialog(null,
                    "Move "+ selectedPassword.identifier +" to...",
                    "Move to...", JOptionPane.PLAIN_MESSAGE, null,
                    choices, // Array of choices
                    choices[tabbedPane.getSelectedIndex()]); // Initial choice

            if (input != null) {
                DirectoryEntry keychainDestination = keychains.get(choicesList.indexOf(input));
                String identifier = selectedPassword.identifier;
                String username = selectedPassword.metadata.get("username");
                SecretKey password =  selectedPassword.password;
                boolean success = directoryController.deletePassword(selectedPassword);
                Optional<String> comments = Optional.ofNullable((selectedPassword.metadata.get("comments")));
                
                if (success){
                	Keychain key = selectedPassword.keychain;
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getName(), 
                				String.format("Password %s removed from keychain <%s>", selectedPassword.identifier, key.getName()));
                	}
                }
                
                Keychain newDestination = this.keychains.get(choicesList.indexOf(input)).readKeychain().get();
                success = directoryController.createPassword(newDestination, identifier, username, password);

                Password addedPassword = newDestination.passwords.get(newDestination.passwords.size()-1);
                if(comments.isPresent())
                    directoryController.updateMetadataEntry(addedPassword,"comments",comments.get());
                
                if (success){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, newDestination.getName(), 
                				String.format("Password %s added to keychain <%s>", selectedPassword.identifier, newDestination.getName()));
                	}
                }

                refreshTabbedPanes();
            }
        });
        movePass.setEnabled(false);
        fileMenu.add(movePass);


        delPass = new JMenuItem("Delete Password");
        passwordMenuItems.add(delPass);
        delPass.addActionListener(e -> {
            int deletePassword = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this password?",
                    "Delete Password", JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (deletePassword == JOptionPane.YES_OPTION) {
            	if (directoryController.deletePassword(selectedPassword)){
            		Keychain key = selectedPassword.keychain;
            		Optional<Account> act = directoryController.getAccount();
            		if (act.isPresent()){
            			keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getName(), 
            					String.format("Password %s removed from keychain <%s>", selectedPassword.identifier, key.getName()));
            		}
            	}
                refreshTabbedPanes();
            }
        });
        delPass.setEnabled(false);
        fileMenu.add(delPass);

        fileMenu.addSeparator();
        menuItem = new JMenuItem("Change Master Password");
        menuItem.addActionListener(e ->{
            keyService.updateMasterKey();
        });
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Create Public Keyfile");
        menuItem.addActionListener((ActionEvent e) ->{
            String username = directoryController.getAccount().get().getUsername();
            JFileChooser fc = new JFileChooser();
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // save to file
                String path = file.getPath();

                RSAPublicKey publicKey = (RSAPublicKey) directoryController.getAccount().get().getKeyPair().getPublic();
                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());

                try {
                    Files.write(Paths.get(path), x509EncodedKeySpec.getEncoded());
                } catch (IOException e1) {
                    JOptionPane.showConfirmDialog(frame, "Unable to write to location: "+path);
                }
            }

        });


        fileMenu.add(menuItem);

        menuItem = new JMenuItem(("Sync"));
        menuItem.addActionListener(e -> serverController.sync(() -> invokeLater(this::refreshTabbedPanes)));
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(e -> frame.dispose());
        fileMenu.add(menuItem);

//        menuItem = new JMenuItem("Refresh");
//        menuItem.addActionListener(e -> refreshTabbedPanes());
//        fileMenu.add(menuItem);


        //KEYCHAIN Menu Item
        JMenu keychainMenu = new JMenu("Keychain");
        menuBar.add(keychainMenu);

        menuItem = new JMenuItem("New Keychain");
        menuItem.addActionListener(e -> {
            String k = (String) JOptionPane.showInputDialog(
                    frame,
                    "New Keychain Name",
                    "New Keychain",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");

            if ((k != null) && (k.length() > 0)) {
                if (directoryController.createKeychain(k)){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, k, String.format("Keychain <%s> created.", k));
                	}
                }
            }
            refreshTabbedPanes();
        });
        keychainMenu.add(menuItem);

        keychainMenu.addSeparator();

        rnKey = new JMenuItem("Rename Keychain");
        keychainMenuItems.add(rnKey)
        rnKey.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(frame,"New keychain name:","Update name",JOptionPane.PLAIN_MESSAGE);
            if (newName!=null){
            	String oldName = getActiveKeychain().getName();
            	
                if (directoryController.renameKeychain(getActiveKeychain(),newName)){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, oldName, 
                				String.format("Keychain <%s> changed name to <%s>.", oldName, newName));
                	}
                }
                refreshTabbedPanes();
            }
        });
        keychainMenu.add(rnKey);

        delKey = new JMenuItem("Delete Keychain");
        keychainMenuItems.add(delKey);
        delKey.addActionListener(e -> {
            int deleteKeychain = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this keychain?",
                    "Delete keychain", JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (deleteKeychain == JOptionPane.YES_OPTION) {
            	Keychain key = getActiveKeychain();
                if (directoryController.deleteKeychain(key)){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, key.getName(), 
                				String.format("Keychain <%s> deleted.", key.getName()));
                	}
                }
                refreshTabbedPanes();
            }
        });
        keychainMenu.add(delKey);


        addKeyPerm = new JMenuItem("Add Keychain Permissions");
        keychainMenuItems.add(addKeyPerm);
        addKeyPerm.addActionListener(e ->{

            JFileChooser fc = new JFileChooser();
            JTextField shareusername = new JTextField();
            JCheckBox writepriv = new JCheckBox();

            final File[] file = new File[1];
            JTextField filename = new JTextField();
            filename.setEditable(false);
            JButton openfile = new JButton("Attach public key file");

            openfile.addActionListener(ee->{
                int returnVal=fc.showOpenDialog(frame);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    file[0] = fc.getSelectedFile();
                    filename.setText(file[0].getName());
                }
            });

            Object[] message = {
                    "Username:", shareusername,
                    "Can edit? ", writepriv,
                    "Public key file: ", filename,
                    openfile
            };

            int option = JOptionPane.showConfirmDialog(frame, message, "Share Keychain: "+getActiveKeychain().getName(), JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                if (shareusername.getText().length() > 0 && filename.getText().length()>0 && file[0].exists()) {
                    DirectoryEntry activeDE = directoryController.getKeychains().get(tabbedPane.getSelectedIndex());
                    String username = shareusername.getText();
                    Boolean readWrite = writepriv.isSelected();
                    //Try to read public key
                    try {
                        byte[] keyBytes = Files.readAllBytes(file[0].toPath());
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(spec);

                        Share newshare = new Share(-1L,username,readWrite, publicKey, false);

                        boolean success = directoryController.shareKeychain(activeDE,newshare);
                        if(success){
                            JOptionPane.showMessageDialog(frame,
                                    "Successfully shared " + activeDE.name+ " with "+ username);
                            
                            Optional<Account> act = directoryController.getAccount();
                            String user = null;
                            if (act.isPresent())
                            	user = act.get().getUsername();
                            keyLog.addCreateShareLogEntry(user, true, getActiveKeychain().getName(), 
                            		String.format("Keychain <%s> shared with user %s", getActiveKeychain().getName(), username));
                        }
                        else{
                            JOptionPane.showMessageDialog(frame,
                                    "Unable to share " + activeDE.name+ " with "+ username);
                        }
                    }catch (IOException | ClassCastException|
                            InvalidKeySpecException |NoSuchAlgorithmException e1){

                        JOptionPane.showMessageDialog(frame,"Unable to read public key file");
                    }
                }
            }
        });
        keychainMenu.add(addKeyPerm);

        rmKeyPerm = new JMenuItem("Remove Keychain Permissions");
        rmKeyPerm.addActionListener(e ->{
            String unshareUser = (String) JOptionPane.showInputDialog(
                    frame,
                    "Who to unshare with?",
                    "Unshare Keychain: "+getActiveKeychain().getName(),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");

            if ((unshareUser != null) && (unshareUser.length() > 0)) {
                DirectoryEntry activeDE = directoryController.getKeychains().get(tabbedPane.getSelectedIndex());
                ArrayList<Share> shareList = (ArrayList<Share>) activeDE.shares;
                Share unshare=null;
                for (Share s:shareList){
                    if (s.username.equals(unshareUser)){
                        unshare=s;
                    }
                }
                if(unshare==null){
                    JOptionPane.showMessageDialog(frame,"Unable to find "+unshareUser+" in shared list",
                            "Unshare error",JOptionPane.PLAIN_MESSAGE);
                }
                else {
                	if (directoryController.unshareKeychain(activeDE, unshare)){

                		Optional<Account> act = directoryController.getAccount();
                		String user = null;
                		if (act.isPresent())
                			user = act.get().getUsername();
                		
                		keyLog.addRemoveShareLogEntry(user, true, getActiveKeychain().getName(), 
                				String.format("Keychain <%s> shared with user %s", getActiveKeychain().getName(), unshareUser));
                	}
                }
            }

        });
        keychainMenu.add(rmKeyPerm);

        showKeyPerm=new JMenuItem("Show key permissions");
        showKeyPerm.addActionListener(e -> {
            String[] columnNames = {"User", "Editor"};
            DirectoryEntry activeDE = directoryController.getKeychains().get(tabbedPane.getSelectedIndex());
            List<Share> shares = activeDE.shares;

            String[][] data = new String[shares.size()][2];
            for (int i = 0; i < shares.size(); i++) {
                data[i][0] = shares.get(i).username;
                if (shares.get(i).isOwner){
                    data[i][0]+= (" (Owner)");
                }
                data[i][1] = (shares.get(i).canWrite? "Y":"");
            }

            JTable table= new JTable(data, columnNames) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JScrollPane scrollPane = new JScrollPane(table);
            Panel panel = new Panel();
            panel.add(scrollPane);

            JOptionPane.showConfirmDialog(frame,panel,"Permissions: "+getActiveKeychain().getName(),
                    JOptionPane.CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        });
        keychainMenu.add(showKeyPerm);

        showKeyLogs = new JMenuItem("Keychain logs");
        showKeyLogs.addActionListener(e->{
            String keyname = getActiveKeychain().getName();
            List logEntries = keyLog.getKeychainLogEntries(keyname);

            JOptionPane.showConfirmDialog(frame,logEntries,keyname+" Log",
                    JOptionPane.CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        });
        keychainMenu.add(showKeyLogs);

        //SETTINGS Menu Item
        JMenu settingsMenu = new JMenu("Settings");
        menuBar.add(settingsMenu);

        menuItem = new JMenuItem("Pwd Gen Settings");
        menuItem.addActionListener( e -> {passwordGenerator.editSettings(frame);});
        settingsMenu.add(menuItem);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                delPass.setEnabled(false);
                movePass.setEnabled(false);
                for (JTable t:tables){
                    t.clearSelection();
                }
            }
        });

        frame.setJMenuBar(menuBar);

        refreshTabbedPanes();
        frame.add(tabbedPane);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    void refreshTabbedPanes() {
        int activeKeychainidx = tabbedPane.getSelectedIndex();
        if (activeKeychainidx==-1 && tabbedPane.getTabCount()>0){
            activeKeychainidx=0;
        }

        tabbedPane.removeAll();
        for (DirectoryEntry entry : this.keychains) {
            Optional<Keychain> keychain = entry.readKeychain();

            if (!keychain.isPresent()) {
                // TODO
            }

            addPanes(keychain.get());
        }

        delPass.setEnabled(false);
        movePass.setEnabled(false);
        if (this.keychains.size() == 0) {
            addPass.setEnabled(false);
            delKey.setEnabled(false);
            rnKey.setEnabled(false);
            addKeyPerm.setEnabled(false);
            rmKeyPerm.setEnabled(false);
            showKeyPerm.setEnabled(false);
            showKeyLogs.setEnabled(false);
        } else {
            addPass.setEnabled(true);
            delKey.setEnabled(true);
            rnKey.setEnabled(true);
            addKeyPerm.setEnabled(true);
            rmKeyPerm.setEnabled(true);
            showKeyPerm.setEnabled(true);
            showKeyLogs.setEnabled(true);
        }
        if(activeKeychainidx>-1 && activeKeychainidx<tabbedPane.getTabCount()){
            tabbedPane.setSelectedIndex(activeKeychainidx);
        }
    }

    private void addPanes(Keychain k) {
        JComponent tabpanel = new JPanel();
        String name = k.getName();
        tabbedPane.addTab(name, null, tabpanel,
                "Keychain " + name);

        //fill table for Keychain k with all of its passwords
        String[] columnNames = {"Title", "Username"};
        List<Password> passwords = k.passwords;

        String[][] data = new String[passwords.size()][2];
        for (int i = 0; i < passwords.size(); i++) {
            data[i][0] = passwords.get(i).identifier;
            Map<String, String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("username"))
                data[i][1] = metadata.get("username");
            else
                data[i][1] = "";
        }

        JTable table= new JTable(data, columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    delPass.setEnabled(true);
                    movePass.setEnabled(true);
                    selectedPassword = getActivePassword(passwords, e);
                } else if (e.getClickCount() == 2) {
                    Password activePassword = getActivePassword(passwords, e);
                    PasswordViewer pv = new PasswordViewer(kv, directoryController, keyLog, getActiveKeychain(), activePassword);
                }
            }
        });


        tables.add(table);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        tabpanel.add(scrollPane);

    }

    private Keychain getActiveKeychain() {
        int activeKeychainidx = tabbedPane.getSelectedIndex();

        Optional<Keychain> keychain = this.keychains.get(activeKeychainidx).readKeychain();

        if (!keychain.isPresent()) {
            // TODO
        }

        return keychain.get();
    }

    private Password getActivePassword(List<Password> passwords, MouseEvent e) {
        JTable target = (JTable) e.getSource();
        int row = target.getSelectedRow();

        try {
            return passwords.get(row);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }
}