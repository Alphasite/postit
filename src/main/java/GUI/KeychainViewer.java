package gui;

import backend.BackingStore;
import backend.BackingStoreImpl;
import backend.KeyService;
import keychain.Directory;

import handler.DirectoryController;
import keychain.DirectoryEntry;
import keychain.Keychain;
import keychain.Password;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

/**
 * Created by jackielaw on 2/27/17.
 */
public class KeychainViewer {
    DirectoryController controller;
    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane=new JTabbedPane();
    //each JTable contains a list of passwords in a particular Keychain
    private ArrayList<JTable> tables = new ArrayList<JTable>();

    public KeychainViewer(){
        // Hardcoding Directory to be a new directory currently
        // Will incorporate loading a directory after account creation is enabled
        KeyService keyService = new GUIKeyService();
        BackingStore backingStore = new BackingStoreImpl(keyService);

        backingStore.init();
        Optional<Directory> directory = backingStore.readDirectory();

        if (!directory.isPresent()) {

        }

        controller = new DirectoryController(directory.get(), keyService);

        createUIComponents(controller.getKeychains());
    }

    public static void main(String[] args)
    {
        KeychainViewer kv = new KeychainViewer();
    }

    /**
     *  creatUIComponents()
     *
     *  Creates the UI with each tabbedPane as a keychain
     *  Each tabbedPane contains a table that holds all the passwords
     *  for that keychain
     *
     *  @param : List<Keychain> keychains
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
        fileMenu.add(menuItem);
        menuItem = new JMenuItem("Logout");
        fileMenu.add(menuItem);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        menuItem = new JMenuItem("Keychain Permissions");
        editMenu.add(menuItem);

        frame.setJMenuBar(menuBar);

        //loop through directory and name will be keychain name (can get from DirectoryEntry or Keychain)
        for (DirectoryEntry entry: keychains) {
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

    private void addPanes(Keychain k){
        JComponent tabpanel = new JPanel();
        String name = k.name;
        tabbedPane.addTab(name,null, tabpanel,
                "Keychain "+name);

        //fill table for Keychain k with all of its passwords
        String[] columnNames = {"Title", "Username"};
        String[][] data = new String[2][k.passwords.size()];
        List<Password> passwords = (ArrayList<Password>) k.passwords;
        for(int i = 0; i< passwords.size(); i++){
            Map<String,String> metadata = passwords.get(i).metadata;
            if (metadata.containsKey("title"))
                data[0][i]=metadata.get("title");
            else
                data[0][i]=metadata.get("title"+i);
            if (metadata.containsKey("username"))
                data[1][i]=metadata.get("username");
            else
                data[1][i]=metadata.get("user"+i);
        }

        JTable table = new JTable(data,columnNames) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable)e.getSource();
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