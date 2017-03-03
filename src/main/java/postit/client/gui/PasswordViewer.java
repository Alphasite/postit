package postit.client.gui;

import postit.shared.Crypto;
import postit.client.keychain.Password;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
/**
 * Created by jackielaw on 2/26/17.
 */
public class PasswordViewer {

    //private Password p;
    private JPanel panel;
    private JTextField titleField;
    private JTextField userField;
    private JPasswordField passField;
    private JTextArea comments;
    private JButton toggleView;
    private JLabel titleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel commLabel;
    private JButton saveButton;

    public PasswordViewer(Password p) {
        // TODO: place custom component creation code here
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
                System.out.print("Will need to save the changes to password");
            }
        });
    }

    public static void main(String[] args) {
        //PasswordViewer pv = new PasswordViewer();
    }

    private void createUIComponents(Password p) {
        // TODO: place custom component creation code here
        JFrame frame = new JFrame("Password");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        Map<String,String> metadata = p.metadata;

        if (metadata.containsKey("title"))
            titleField.setText(metadata.get("title"));
        else
            titleField.setText("title");
        if (metadata.containsKey("username"))
            userField.setText(metadata.get("username"));
        else
            userField.setText("user");
        if (metadata.containsKey("comments"))
            comments.setText(metadata.get("comments"));
        else
            comments.setText("comments");

        byte[] bytes = Crypto.secretKeyToBytes(p.password);
        passField.setText(new String(bytes));
    }
}
