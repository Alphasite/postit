package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Created by jackielaw on 2/27/17.
 */
public class KeychainViewer {
    private JMenuBar menuBar;
    private JMenuItem menuItem;

    private JTabbedPane tabbedPane=new JTabbedPane();
    //each JTable contains a list of passwords in a particular Keychain
    private ArrayList<JTable> tables = new ArrayList<JTable>();

    public KeychainViewer(){
        createUIComponents();

    }

    public static void main(String[] args)
    {
        KeychainViewer kv = new KeychainViewer();
    }
    //Buttons to add: Log out,change permissions, add password

    private void createUIComponents() {
        // TODO: place custom component creation code here
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
        addPanes("MyPane1");
        addPanes("MyPane2");

        frame.add(tabbedPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void addPanes(String name){
        JComponent tabpanel = new JPanel();
        tabbedPane.addTab(name,null, tabpanel,
                "A Keychain");

        //add table will be from the Keychain
        String[] columnNames = {"Title", "Username"};
        String[][] data = {{"mytitle1","myuser1"},{"mytitle2","myuser2"}};

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
                    PasswordViewer pv = new PasswordViewer("Password"+String.valueOf(row));
                }
            }
        });
        tables.add(table);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        tabpanel.add(scrollPane);
    }
}