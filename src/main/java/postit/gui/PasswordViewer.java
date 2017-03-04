package postit.gui;

import postit.backend.Crypto;
import postit.handler.DirectoryController;
import postit.keychain.Keychain;
import postit.keychain.Password;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
/**
 * Created by jackielaw on 2/26/17.
 */
public class PasswordViewer {

    private JPanel panel;
    private JTextField titleField;
    private JTextField userField;
    private JPasswordField passField;
    private JTextArea comments;
    private JButton toggleView;
    private JButton saveButton;
    private JButton deleteButton;

    public PasswordViewer(DirectoryController c, Keychain k, Password p) {
        // TODO: place custom component creation code here
        titleField = new JTextField();
        titleField.setEditable(false);
        userField = new JTextField();
        userField.setEditable(false);
        passField = new JPasswordField();
        comments = new JTextArea();
        comments.setEditable(false);
        toggleView = new JButton("Toggle");

        saveButton = new JButton("Save");

        createUIComponents(p);
        toggleView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(passField.getEchoChar()==(char)0)
                    passField.setEchoChar((char)9679);
                else
                    passField.setEchoChar((char)0);
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newKey = String.valueOf(passField.getPassword());
                c.updatePassword(p,Crypto.secretKeyFromBytes(newKey.getBytes()));
            }
        });
    }


    private void createUIComponents(Password p) {
        // TODO: place custom component creation code here
        JFrame frame = new JFrame("Password");


        Map<String,String> metadata = p.metadata;

        titleField.setText(p.identifier);
        if (metadata.containsKey("username"))
            userField.setText(metadata.get("username"));
        else
            userField.setText("user");

        byte[] bytes = Crypto.secretKeyToBytes(p.password);
        passField.setText(new String(bytes));
        if (metadata.containsKey("comments"))
            comments.setText(metadata.get("comments"));
        else
            comments.setText("");

        panel = new JPanel(new GridLayout(5,3));
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        panel.setBorder(padding);

        panel.add(new JLabel("Title"));
        panel.add(titleField);
        panel.add(new JLabel(""));
        panel.add(new JLabel("Username"));
        panel.add(userField);
        panel.add(new JLabel(""));
        panel.add(new JLabel("Password"));
        panel.add(passField);
        panel.add(toggleView);
        panel.add(new JLabel("Comments"));
        panel.add(comments);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(saveButton);
        panel.add(new JLabel(""));

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
