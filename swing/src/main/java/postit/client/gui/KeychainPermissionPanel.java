package postit.client.gui;

import postit.client.controller.DirectoryController;
import postit.client.keychain.Keychain;
import postit.client.keychain.Share;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jackielaw on 4/17/17.
 */
public class KeychainPermissionPanel extends JPanel {

    private JTabbedPane tabbedPane = new JTabbedPane();
    private JTable editorTable = new JTable();
    private JTable readerTable = new JTable();
    private List<Share> editors = new ArrayList<>();
    private List<Share> readers = new ArrayList<>();

    private DirectoryController directoryController;
    private Keychain keychain;

    public KeychainPermissionPanel(DirectoryController dr, Keychain k) {
        directoryController=dr;
        keychain=k;
        // TODO: set editors
        // TODO: set readers
        setPreferredSize(new Dimension(500,350));
        createUIComponents();

        setVisible(true);


    }

    private void createUIComponents() {
        //OWNER
        JLabel label_owner = new JLabel("Owner: ");
        this.add(label_owner);

        String[] columnNames = {"Username", "Publickey"};

        //EDITORS
        JComponent tabpanel = new JPanel();
        tabbedPane.addTab("Editors", tabpanel);
        String[][] data = new String[editors.size()][2];


        //READERS
        tabpanel = new JPanel();
        tabbedPane.addTab("Editors", tabpanel);
        data = new String[readers.size()][2];
    }

}
