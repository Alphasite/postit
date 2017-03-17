
package postit.client.gui;


import postit.client.backend.BackingStore;
import postit.client.backend.KeyService;
import postit.client.controller.DirectoryController;
import postit.client.controller.ServerController;
import postit.client.keychain.Directory;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.Keychain;
import postit.client.keychain.Password;
import postit.communication.Client;
import postit.communication.Server;
import postit.shared.Crypto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by jackielaw on 2/27/17.
 */
public class KeychainViewer {
    BackingStore backingStore;
    KeyService keyService;

    KeychainViewer kv = this;
    Directory dir;
    DirectoryController directoryController;
    ServerController serverController;
    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane = new JTabbedPane();
    private List<DirectoryEntry> keychains;
    //each JTable contains a list of passwords in a particular Keychain
    private List<JTable> tables = new ArrayList<>();

    private Password selectedPassword;
    private JMenuItem addPass;
    private JMenuItem delPass;
    private JMenuItem delKey;

    public KeychainViewer(BackingStore backingStore, KeyService keyService) {

        this.backingStore = backingStore;
        this.keyService = keyService;

        Optional<Directory> directory = backingStore.readDirectory();

        if (!directory.isPresent()) {
            JOptionPane.showMessageDialog(null,
                    "Could not load directory. Master password may be wrong or data has been compromised");
        } else {
            dir = directory.get();
            directoryController = new DirectoryController(directory.get(), backingStore, keyService);
            this.keychains = directoryController.getKeychains();

            int rePort = 2048;
            int outPort = 4880;

            Client processor = new Client(outPort, false);
            Server receiver = new Server(rePort, false, processor);
            serverController = new ServerController(processor,receiver,directoryController,keyService);
            createUIComponents();
        }

    }

    public static void main(String[] args) {
        // FOR CONNECTING TO THE POSTIT SERVER
        Client sender = new Client(2048, false);
        Server listener = new Server(4880, false, sender);

        Thread t1 = new Thread(listener);
        Thread t2 = new Thread(sender);

        t2.start();
        t1.start();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GUIKeyService keyService = new GUIKeyService();
                BackingStore backingStore = new BackingStore(keyService);

                if (!Crypto.init()) {
                    // TODO
                }
                if (!backingStore.init()) {
                    // TODO
                }

                KeychainViewer kv = new KeychainViewer(backingStore, keyService);
            }

        });
    }

    /**
     * creatUIComponents()
     * <p>
     * Creates the UI with each tabbedPane as a keychain
     * Each tabbedPane contains a table that holds all the passwords
     * for that keychain
     *
     */
    private void createUIComponents() {
        JFrame frame = new JFrame("Keychain");
        frame.setLayout(new GridLayout());
        frame.setMinimumSize(new Dimension(520,485));
        menuBar = new JMenuBar();

        //FILE Menu Items
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        addPass = new JMenuItem("New Password");
        addPass.addActionListener(e -> {
            JTextField newtitle = new JTextField();
            JTextField newpassword = new JPasswordField();
            Object[] message = {
                    "Title:", newtitle,
                    "Password:", newpassword
            };

            int option = JOptionPane.showConfirmDialog(frame, message, "New Password", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                if(newtitle.getText().length() > 0 && newpassword.getText().length() > 0) {
                    directoryController.createPassword(getActiveKeychain(),
                                                newtitle.getText(),
                                                Crypto.secretKeyFromBytes(newpassword.getText().getBytes()));
                }
            }
            refreshTabbedPanes();
        });
        addPass.setEnabled(false);
        fileMenu.add(addPass);

        delPass = new JMenuItem("Delete Password");
        delPass.addActionListener(e -> {
            int deletePassword = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this password?",
                    "Delete Password", JOptionPane.YES_NO_OPTION);
            if (deletePassword==JOptionPane.YES_OPTION){
                directoryController.deletePassword(selectedPassword);
                refreshTabbedPanes();
            }
        });
        delPass.setEnabled(false);
        fileMenu.add(delPass);

        fileMenu.addSeparator();
        menuItem = new JMenuItem("Change Master Password");
        menuItem.setEnabled(false);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem(("Refresh"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTabbedPanes();
            }
        });
        fileMenu.add(menuItem);


        menuItem = new JMenuItem(("Sync"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverController.sync();
            }
        });
//        menuItem.setEnabled(false);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        fileMenu.add(menuItem);

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
                directoryController.createKeychain(k);
            }
            refreshTabbedPanes();
        });
        keychainMenu.add(menuItem);

        delKey = new JMenuItem("Delete Keychain");
        delKey.addActionListener(e -> {
            int deleteKeychain = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this keychain?",
                                                            "Delete keychain", JOptionPane.YES_NO_OPTION);
            if (deleteKeychain==JOptionPane.YES_OPTION){
                directoryController.deleteKeychain(getActiveKeychain());
                refreshTabbedPanes();
            }

        });
        keychainMenu.add(delKey);

        keychainMenu.addSeparator();

        menuItem = new JMenuItem("Keychain Permissions");
        menuItem.setEnabled(false);
        keychainMenu.add(menuItem);

        frame.setJMenuBar(menuBar);

        refreshTabbedPanes();
        frame.add(tabbedPane);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

     void refreshTabbedPanes(){
        tabbedPane.removeAll();
        for (DirectoryEntry entry : this.keychains) {
            Optional<Keychain> keychain = entry.readKeychain();

            if (!keychain.isPresent()) {
                // TODO
            }

            addPanes(keychain.get());
        }

        delPass.setEnabled(false);
        if(this.keychains.size()==0) {
            addPass.setEnabled(false);
            delKey.setEnabled(false);
        }
        else{
             addPass.setEnabled(true);
             delKey.setEnabled(true);
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
            data[i][0]=passwords.get(i).identifier;
            Map<String, String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("username"))
                data[i][1] = metadata.get("username");
            else
                data[i][1] = "";
        }

        JTable table = new JTable(data, columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==1){
                    delPass.setEnabled(true);
                    selectedPassword=getActivePassword(passwords,e);
                }
                else if (e.getClickCount() == 2) {
                    Password activePassword = getActivePassword(passwords, e);
                    PasswordViewer pv = new PasswordViewer(kv, directoryController,getActiveKeychain(),activePassword);
                }
            }
        });


        tables.add(table);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        tabpanel.add(scrollPane);

    }

    private Keychain getActiveKeychain(){
        int activeKeychainidx = tabbedPane.getSelectedIndex();
        Optional<Keychain> keychain = this.keychains.get(activeKeychainidx).readKeychain();

        if (!keychain.isPresent()) {
            // TODO
        }

        return keychain.get();
    }

    private Password getActivePassword(List<Password> passwords, MouseEvent e){
        JTable target = (JTable) e.getSource();
        int row = target.getSelectedRow();
        return passwords.get(row);
    }
}