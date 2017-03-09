
package postit.client.gui;

import postit.client.backend.BackingStore;
import postit.client.backend.BackingStoreImpl;
import postit.client.backend.KeyService;
import postit.client.controller.DirectoryController;
import postit.client.keychain.Directory;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.Keychain;
import postit.client.keychain.Password;
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

    Directory dir;
    DirectoryController controller;
    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane = new JTabbedPane();
    private List<DirectoryEntry> keychains;
    //each JTable contains a list of passwords in a particular Keychain
    private List<JTable> tables = new ArrayList<JTable>();

    private Password selectedPassword;
    private JMenuItem addPass;
    private JMenuItem delPass;
    private JMenuItem delKey;

    public KeychainViewer(BackingStore backingStore, KeyService keyService) {

        this.backingStore = backingStore;
        this.keyService = keyService;

        Optional<Directory> directory = backingStore.readDirectory();

        if (!directory.isPresent()) {
            JOptionPane.showMessageDialog(null, "Could not load directory");
        } else {
            dir = directory.get();
            controller = new DirectoryController(directory.get(), keyService);
            this.keychains = controller.getKeychains();
            createUIComponents();
        }

    }

    public static void main(String[] args) {
        GUIKeyService keyService = new GUIKeyService();
        BackingStoreImpl backingStore = new BackingStoreImpl(keyService);

        if (!Crypto.init()) {
            // TODO
        }

        if (!backingStore.init()) {
            // TODO
        }

        KeychainViewer kv = new KeychainViewer(backingStore, keyService);
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
        addPass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField newtitle = new JTextField();
                JTextField newpassword = new JPasswordField();
                Object[] message = {
                        "Title:", newtitle,
                        "Password:", newpassword
                };

                int option = JOptionPane.showConfirmDialog(frame, message, "New Password", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    if(newtitle!=null && newtitle.getText().length()>0
                            && newpassword!=null && newpassword.getText().length()>0) {
                        controller.createPassword(getActiveKeychain(),
                                                    newtitle.getText(),
                                                    Crypto.secretKeyFromBytes(newpassword.getText().getBytes()));
                    }
                }
                refreshTabbedPanes();
            }
        });
        addPass.setEnabled(false);
        fileMenu.add(addPass);

        delPass = new JMenuItem("Delete Password");
        delPass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int deletePassword = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this password?",
                        "Delete Password", JOptionPane.YES_NO_OPTION);
                if (deletePassword==JOptionPane.YES_OPTION){
                    controller.deletePassword(selectedPassword);
                    refreshTabbedPanes();
                }
            }
        });
        delPass.setEnabled(false);
        fileMenu.add(delPass);

        fileMenu.addSeparator();
        menuItem = new JMenuItem("Change Master Password");
        menuItem.setEnabled(false);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem(("Sync"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        fileMenu.add(menuItem)

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
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String k = (String) JOptionPane.showInputDialog(
                        frame,
                        "New Keychain Name",
                        "New Keychain",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");

                if ((k != null) && (k.length() > 0)) {
                    controller.createKeychain(k);
                }
                refreshTabbedPanes();
            }
        });
        keychainMenu.add(menuItem);

        delKey = new JMenuItem("Delete Keychain");
        delKey.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int deleteKeychain = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this keychain?",
                                                                "Delete keychain", JOptionPane.YES_NO_OPTION);
                if (deleteKeychain==JOptionPane.YES_OPTION){
                    controller.deleteKeychain(getActiveKeychain());
                    refreshTabbedPanes();
                }

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

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        List<Password> passwords = (ArrayList<Password>) k.passwords;

        String[][] data = new String[passwords.size()][2];
        for (int i = 0; i < passwords.size(); i++) {
            data[i][0]=passwords.get(i).identifier;
            Map<String, String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("username"))
                data[i][1] = metadata.get("username");
            else
                data[i][1] = metadata.get("user" + i);
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
                    PasswordViewer pv = new PasswordViewer(controller,getActiveKeychain(),activePassword);
                }
                table.revalidate();
                table.repaint();
            }
        });


        tables.add(table);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        tabpanel.add(scrollPane);

    }

    private Keychain getActiveKeychain(){
        int activeKeychainidx = tabbedPane.getSelectedIndex();
        return this.keychains.get(activeKeychainidx).readKeychain().get();
    }

    private Password getActivePassword(List<Password> passwords, MouseEvent e){
        JTable target = (JTable) e.getSource();
        int row = target.getSelectedRow();
        return passwords.get(row);
    }
}