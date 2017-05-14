
package postit.client.gui;


import org.json.JSONObject;
import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.controller.DirectoryController;
import postit.client.controller.ServerController;
import postit.client.keychain.*;
import postit.client.log.AuthenticationLog;
import postit.client.log.KeychainLog;
import postit.client.passwordtools.Classify;
import postit.client.passwordtools.PasswordGenerator;
import postit.shared.AuditLog;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.time.Duration;
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
    BackingStore backingStore;

    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane = new JTabbedPane();
    private List<DirectoryEntry> keychains;
    //each JTable contains a list of passwords in a particular Keychain
    private List<JTable> tables = new ArrayList<>();

    private Password selectedPassword;
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

    public KeychainViewer(ServerController serverController, BackingStore backingStore, KeyService keyService, KeychainLog keyLog, AuthenticationLog authLog) {

        this.serverController = serverController;
        this.backingStore = backingStore;

        Optional<Directory> directory = backingStore.readDirectory();


        if (!directory.isPresent()) {
            JOptionPane.showMessageDialog(null,
                    "Could not load directory. Master password may be wrong or data has been compromised");
            
            authLog.addAuthenticationLogEntry("N/A", false, "Login credentials are invalid");
        } else {
            directoryController = new DirectoryController(directory.get(), backingStore, keyService);
            this.keychains = directoryController.getKeychains();

            serverController.setDirectoryController(directoryController);
            createUIComponents();
            
            Optional<Account> act = directoryController.getAccount();
            if (act.isPresent()){
            	authLog.addAuthenticationLogEntry(act.get().getUsername(), true, "Login successful");
            }
            StringBuffer overduePasswords = new StringBuffer("");
            for(DirectoryEntry directoryEntry:directoryController.getKeychains()) {
                List<Password>expiredPasswords = directoryController.getExpired(directoryEntry.readKeychain().get(), Duration.ofDays(365));
                for (Password p:expiredPasswords){
                    overduePasswords.append(p.getTitle());
                    overduePasswords.append("\n");
                }
            }
            if(overduePasswords.length()!=0){
                JOptionPane.showMessageDialog(null,
                        "Passwords that have not updated for a year: \n"+overduePasswords.toString()+"To stop update messages, re-save your passwords",
                        "Overdue passwords",JOptionPane.PLAIN_MESSAGE);
            }
        }

        passwordGenerator = directoryController.getPasswordGenerator();
        classify = new Classify();
        this.keyService = keyService;
        
        this.keyLog = keyLog;

        backingStore.save();
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
        menuBar = new JMenuBar();

        //FILE Menu Items
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        addPass = new JMenuItem("New Password");
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
                            Crypto.secretKeyFromBytes(newpassword.getText().getBytes(StandardCharsets.UTF_8)));
                    if (success){
                    	Keychain key = getActiveKeychain();
                    	Optional<Account> act = directoryController.getAccount();
                    	if (act.isPresent()){
                    		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getServerId(), 
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
        movePass.addActionListener(e -> {
            ArrayList<String> choicesList = new ArrayList<String>();
            for(DirectoryEntry de : keychains){
                choicesList.add(de.name);
            }
            String[] choices = choicesList.toArray(new String[choicesList.size()]);
            String passwordTitle = selectedPassword.metadata.get("title");
            if (passwordTitle==null)
                passwordTitle="";
            String input =(String) JOptionPane.showInputDialog(null,
                    "Move "+ passwordTitle +" to...",

                    "Move to...", JOptionPane.PLAIN_MESSAGE, null,
                    choices, // Array of choices
                    choices[tabbedPane.getSelectedIndex()]); // Initial choice

            if (input != null) {
                DirectoryEntry keychainDestination = keychains.get(choicesList.indexOf(input));
                String title = selectedPassword.getTitle();
                String username = selectedPassword.metadata.get("username");
                SecretKey password =  selectedPassword.password;
                boolean success = directoryController.deletePassword(selectedPassword);
                Optional<String> comments = Optional.ofNullable((selectedPassword.metadata.get("comments")));
                
                if (success){
                	Keychain key = selectedPassword.keychain;
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getServerId(), 
                				String.format("Password %s removed from keychain <%s>", selectedPassword.getTitle(), key.getName()));
                	}
                }
                
                Keychain newDestination = this.keychains.get(choicesList.indexOf(input)).readKeychain().get();

                directoryController.createPassword(newDestination,
                        title, username, password);


                Password addedPassword = newDestination.passwords.get(newDestination.passwords.size()-1);
                if(comments.isPresent())
                    directoryController.updateMetadataEntry(addedPassword,"comments",comments.get());
                
                if (success){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, newDestination.getServerId(), 
                				String.format("Password %s added to keychain <%s>", selectedPassword.getTitle(), newDestination.getName()));
                	}
                }

                refreshTabbedPanes();
            }
        });
        movePass.setEnabled(false);
        fileMenu.add(movePass);


        delPass = new JMenuItem("Delete Password");
        delPass.addActionListener(e -> {
            int deletePassword = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this password?",
                    "Delete Password", JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (deletePassword == JOptionPane.YES_OPTION) {
            	if (directoryController.deletePassword(selectedPassword)){
            		Keychain key = selectedPassword.keychain;
            		Optional<Account> act = directoryController.getAccount();
            		if (act.isPresent()){
            			keyLog.addUpdateKeychainLogEntry(act.get().getUsername(), true, key.getServerId(), 
            					String.format("Password %s removed from keychain <%s>", selectedPassword.getTitle(), key.getName()));
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
            if (!backingStore.writeKeypair(directoryController.getAccount().get())) {
                JOptionPane.showConfirmDialog(frame, "Unable to write public keyfile.");
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
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, directoryController.getKeychain(k).get().getServerId(),
                				String.format("Keychain <%s> created.", k));
                	}
                }
            }
            refreshTabbedPanes();
        });
        keychainMenu.add(menuItem);

        keychainMenu.addSeparator();

        rnKey = new JMenuItem("Rename Keychain");
        rnKey.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(frame,"New keychain name:","Update name",JOptionPane.PLAIN_MESSAGE);
            if (newName!=null){
            	String oldName = getActiveKeychain().getName();
            	
                if (directoryController.renameKeychain(getActiveKeychain(),newName)){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, getActiveKeychain().getServerId(), 
                				String.format("Keychain <%s> changed name to <%s>.", oldName, newName));
                	}
                }
                refreshTabbedPanes();
            }
        });
        keychainMenu.add(rnKey);

        delKey = new JMenuItem("Delete Keychain");
        delKey.addActionListener(e -> {
            int deleteKeychain = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this keychain?",
                    "Delete keychain", JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (deleteKeychain == JOptionPane.YES_OPTION) {
            	Keychain key = getActiveKeychain();
                if (directoryController.deleteKeychain(key)){
                	Optional<Account> act = directoryController.getAccount();
                	if (act.isPresent()){
                		keyLog.addCreateKeychainLogEntry(act.get().getUsername(), true, key.getServerId(), 
                				String.format("Keychain <%s> deleted.", key.getName()));
                	}
                }
                refreshTabbedPanes();
            }
        });
        keychainMenu.add(delKey);


        addKeyPerm = new JMenuItem("Add Keychain Permissions");
        addKeyPerm.addActionListener(e ->{

            JFileChooser fc = new JFileChooser();
            JTextField shareusername = new JTextField();
            JCheckBox writepriv = new JCheckBox();
//            String[] privs = {"Read","Write","Execute"};
//            JComboBox writepriv = new JComboBox();


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
//                    String readWrite = (String)priv.getSelectedItem();
                    //Try to read public key
                    Optional<List<RSAPublicKey>> keys = backingStore.readPublicKey(file[0].toPath());

                    if (!keys.isPresent()) {
                        // TODO
                        JOptionPane.showMessageDialog(frame,"Unable to read public key file");
                        System.err.println("Error loading public key");
                        return;
                    }

                    RSAPublicKey encryptionKey = keys.get().get(0);
                    RSAPublicKey signingKey = keys.get().get(1);

                    Share newshare = new Share(-1L, username, readWrite, encryptionKey, signingKey, false);

                    boolean success = directoryController.shareKeychain(activeDE,newshare);
                    if (success) {
                        JOptionPane.showMessageDialog(frame,
                                "Successfully shared " + activeDE.name+ " with "+ username);

                        Optional<Account> act = directoryController.getAccount();
                        String user = null;
                        if (act.isPresent())
                            user = act.get().getUsername();
                        keyLog.addCreateShareLogEntry(user, true, getActiveKeychain().getServerId(),
                                String.format("Keychain <%s> shared with user %s", getActiveKeychain().getName(), username));
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Unable to share " + activeDE.name+ " with "+ username);
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
                		
                		keyLog.addRemoveShareLogEntry(user, true, getActiveKeychain().getServerId(), 
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
            long keyId = getActiveKeychain().getServerId();

            String[] columnNames = {"Time","Event","Username","Keychain Name","Status","Message"};
            List<AuditLog.LogEntry> logEntries = keyLog.getKeychainLogEntries(keyId);
            String[][] data = new String[logEntries.size()][6];

            for (int i = 0; i < logEntries.size(); i++) {
                data[i][0] = String.valueOf(new Timestamp(logEntries.get(i).time));
                data[i][1] = String.valueOf(logEntries.get(i).event);
                data[i][2] = String.valueOf(logEntries.get(i).username);
                data[i][3] = String.valueOf(logEntries.get(i).keychainId);
                data[i][4] = String.valueOf(logEntries.get(i).status);
                data[i][5] = String.valueOf(logEntries.get(i).message);
            }

            JTable table= new JTable(data, columnNames) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };


            for (int column = 0; column < table.getColumnCount(); column++)
            {
                TableColumn tableColumn = table.getColumnModel().getColumn(column);
                int preferredWidth = tableColumn.getMinWidth();
                int maxWidth = tableColumn.getMaxWidth();

                for (int row = 0; row < table.getRowCount(); row++)
                {
                    TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
                    Component c = table.prepareRenderer(cellRenderer, row, column);
                    int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
                    preferredWidth = Math.max(preferredWidth, width);

                    //  We've exceeded the maximum width, no need to check other rows

                    if (preferredWidth >= maxWidth)
                    {
                        preferredWidth = maxWidth;
                        break;
                    }
                }

                tableColumn.setPreferredWidth( preferredWidth );
            }

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(1000,300));
            Panel panel = new Panel();
            panel.add(scrollPane);

            JOptionPane.showConfirmDialog(frame,panel,keyId+" Log",
                    JOptionPane.CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        });
        keychainMenu.add(showKeyLogs);

        //SETTINGS Menu Item

        JMenu settingsMenu = new JMenu("Settings");
        menuBar.add(settingsMenu);


        menuItem = new JMenuItem("Account Settings");
        menuItem.addActionListener(e -> {

            JTextField username = new JTextField(directoryController.getAccount().get().getUsername());
            username.setEnabled(false);
            JTextField firstName = new JTextField();
            JTextField lastName = new JTextField();
            JTextField email = new JTextField();
            JTextField phoneNumber = new JTextField();
            JPasswordField password = new JPasswordField();

            Object[] message = {
                    "Username:", username,
                    "First name:", firstName,
                    "Last name:", lastName,
                    "Email:", email,
                    "Phone number:", phoneNumber,
                    "Password:", password
            };

            int option = JOptionPane.showConfirmDialog(frame, message, "Edit Account Settings",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                if(firstName.getText().length()>0||
                        lastName.getText().length()>0 || email.getText().length()>0 ||
                        phoneNumber.getText().length()>0 || password.getPassword().length>0){
                    String key = null;
                    key = JOptionPane.showInputDialog(null, "Enter password", "", JOptionPane.PLAIN_MESSAGE);
                    if (key!=null && serverController.authenticate(new Account(username.getText(),key))){
                        if(username.getText().length()>0){
                            //update username
                        }
                        if (firstName.getText().length()>0){
                            //update first name
                        }
                        if (lastName.getText().length()>0){
                            //update last name
                        }
                        if(email.getText().length()>0){
                            //update email
                        }
                        if(phoneNumber.getText().length()>0){
                            //update phone number
                        }
                        if(password.getPassword().length>0){
                            //update password
                        }
                    }
                    else{ //wrong password
                        JOptionPane.showMessageDialog(null, "Password is invalid. No updates were made");
                    }
                }
            }
        });
        settingsMenu.add(menuItem);

        menuItem = new JMenuItem("Pwd Gen Settings");
        menuItem.addActionListener( e -> {
            this.editSettings(passwordGenerator, frame);
            directoryController.setPasswordGenerator(passwordGenerator);
        });

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
            Map<String, String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("title"))
                data[i][0] = metadata.get("title");
            else
                data[i][0] = "";

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

    public void editSettings(PasswordGenerator generator, JFrame frame){
        SpinnerNumberModel model = new SpinnerNumberModel();
        model.setMaximum(256);
        model.setMinimum(8);

        JSpinner newLength = new JSpinner(model);
        newLength.setValue(generator.activeConfiguration.passwordlength);

        JCheckBox upper = new JCheckBox();
        upper.setText("Upper case (A-Z)?");
        upper.setHorizontalTextPosition(SwingConstants.LEFT);
        if(generator.activeConfiguration.useUpper)
            upper.setSelected(true);

        JCheckBox lower = new JCheckBox();
        lower.setText("Lower case (a-z)?");
        lower.setHorizontalTextPosition(SwingConstants.LEFT);
        if(generator.activeConfiguration.useLower)
            lower.setSelected(true);

        JCheckBox numbers = new JCheckBox();
        numbers.setText("Number (0-9)?");
        numbers.setHorizontalTextPosition(SwingConstants.LEFT);
        if(generator.activeConfiguration.useNumbers)
            numbers.setSelected(true);

        JCheckBox symbols = new JCheckBox();
        symbols.setText("Symbols?");
        symbols.setHorizontalTextPosition(SwingConstants.LEFT);
        if(generator.activeConfiguration.useSymbols)
            symbols.setSelected(true);
        JTextField permittedSymbols = new JTextField(generator.activeConfiguration.SYMBOLS);
        symbols.addActionListener(e->{
            if (symbols.isSelected())
                permittedSymbols.setEnabled(true);
            else
                permittedSymbols.setEnabled(false);
        });
        permittedSymbols.addActionListener(e->{
            permittedSymbols.setText(generator.removeDuplicates(permittedSymbols.getText()));
        });

        ArrayList<Object>  message = new ArrayList<Object>();
        message.add("Length");
        message.add(newLength);
        message.add(upper);
        message.add(lower);
        message.add(numbers);
        message.add(symbols);
        message.add(permittedSymbols);
        do{
            int option = JOptionPane.showConfirmDialog(frame, message.toArray(), "Password Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                passwordGenerator.activeConfiguration.passwordlength = (int) newLength.getValue();
                passwordGenerator.activeConfiguration.useUpper = upper.isSelected();
                passwordGenerator.activeConfiguration.useLower = lower.isSelected();
                passwordGenerator.activeConfiguration.useNumbers = numbers.isSelected();
                passwordGenerator.activeConfiguration.useSymbols = symbols.isSelected();
                permittedSymbols.setText(passwordGenerator.removeDuplicates(permittedSymbols.getText()));
                passwordGenerator.activeConfiguration.SYMBOLS=permittedSymbols.getText();
            }
            message.add("Some chars must be selected");

        } while (!(passwordGenerator.activeConfiguration.useUpper
                || passwordGenerator.activeConfiguration.useLower
                || passwordGenerator.activeConfiguration.useNumbers
                || passwordGenerator.activeConfiguration.useSymbols)
        );
    }
}