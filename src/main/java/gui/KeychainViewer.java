package gui;

import apple.security.KeychainStore;
import backend.BackingStore;
import backend.BackingStoreImpl;
import backend.Crypto;
import backend.KeyService;
import cli.App;
import cli.CLIKeyService;
import handler.DirectoryController;
import keychain.Directory;
import keychain.DirectoryEntry;
import keychain.Keychain;
import keychain.Password;

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
    //each JTable contains a list of passwords in a particular Keychain
    private ArrayList<JTable> tables = new ArrayList<JTable>();

    public KeychainViewer(BackingStore backingStore, KeyService keyService) {
        // Hardcoding Directory to be a new directory currently
        // Will incorporate loading a directory after account creation is enabled

        this.backingStore = backingStore;
        this.keyService = keyService;

        Optional<Directory> directory = backingStore.readDirectory();

        if (!directory.isPresent()) {
            JOptionPane.showMessageDialog(null, "Could not load directory");
        } else {
            dir = directory.get();
            controller = new DirectoryController(directory.get(), keyService);

            createUIComponents(controller.getKeychains());
        }

    }

    public static void main(String[] args) {
        CLIKeyService keyService = new CLIKeyService();
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
     * @param : List<Keychain> keychains
     */
    private void createUIComponents(List<DirectoryEntry> keychains) {
        JFrame frame = new JFrame("Keychain");
        frame.setLayout(new GridLayout());
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        menuItem = new JMenuItem("New Password");

        fileMenu.add(menuItem);
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
                        "name");

                if ((k != null) && (k.length() > 0)) {
                    controller.createKeychain(k);
                    return;
                }

            }
        });
        fileMenu.add(menuItem);
        menuItem = new JMenuItem("Logout");
        fileMenu.add(menuItem);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        menuItem = new JMenuItem("Keychain Permissions");
        editMenu.add(menuItem);

        frame.setJMenuBar(menuBar);

        //loop through directory and name will be keychain name (can get from DirectoryEntry or Keychain)
        for (DirectoryEntry entry : keychains) {
            Optional<Keychain> keychain = entry.readKeychain();

            if (!keychain.isPresent()) {
                // TODO
            }

            addPanes(keychain.get());
        }

        frame.add(tabbedPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void addPanes(Keychain k) {
        JComponent tabpanel = new JPanel();
        String name = k.name;
        tabbedPane.addTab(name, null, tabpanel,
                "Keychain " + name);

        //fill table for Keychain k with all of its passwords
        String[] columnNames = {"Title", "Username"};
        String[][] data = new String[2][k.passwords.size()];
        List<Password> passwords = (ArrayList<Password>) k.passwords;
        for (int i = 0; i < passwords.size(); i++) {
            Map<String, String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("title"))
                data[0][i] = metadata.get("title");
            else
                data[0][i] = metadata.get("title" + i);
            if (metadata.containsKey("username"))
                data[1][i] = metadata.get("username");
            else
                data[1][i] = metadata.get("user" + i);
        }

        JTable table = new JTable(data, columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    int row = target.getSelectedRow();
                    PasswordViewer pv = new PasswordViewer(k.passwords.get(row));
                }
            }
        });
        tables.add(table);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        tabpanel.add(scrollPane);
    }
}