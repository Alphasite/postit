package postit.client.gui;


import org.bouncycastle.util.Arrays;
import postit.client.controller.DirectoryController;
import postit.client.keychain.Account;
import postit.client.keychain.Keychain;
import postit.client.keychain.Password;
import postit.client.log.KeychainLog;
import postit.shared.Crypto;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
/**
 * Created by jackielaw on 2/26/17.
 */
public class PasswordViewer {
    private JFrame frame;
    private JPanel panel;
    private JTextField titleField;
    private JTextField userField;
    private JPasswordField passField;
    private JTextArea comments;
    private JButton toggleView;
    private JButton saveButton;

    public PasswordViewer(KeychainViewer kv, DirectoryController c, KeychainLog kl, Keychain k, Password p) {
        // TODO: place custom component creation code here
        titleField = new JTextField();
        userField = new JTextField();
        passField = new JPasswordField();
        comments = new JTextArea(7,10);
        toggleView = new JButton("<o>");

        saveButton = new JButton("Save");

        createUIComponents(p);
        toggleView.addActionListener(e -> {
            if(passField.getEchoChar()==(char)0)
                passField.setEchoChar((char)9679); //(char)9679 is the black dot symbol
            else
                passField.setEchoChar((char)0);
        });
        saveButton.addActionListener(e -> {
        	
        	Optional<Account> act = c.getAccount();
        	String username = null;
        	if (act.isPresent()){
        		username = act.get().getUsername();
        	}
        	
        	String oldTitle = p.getTitle();
        	long keyId = k.getServerId();
        	String keyName = k.getName();
        	
            String newTitle = String.valueOf(titleField.getText());
            boolean success = c.updatePasswordTitle(p, newTitle);
            
            if (success && username != null && !newTitle.equals(oldTitle)){
            	kl.addUpdateKeychainLogEntry(username, true, keyId, 
            			String.format("Password %s changed name to %s in keychain <%s>", oldTitle, newTitle, keyName));
            }

            String oldUser = p.metadata.get("username");
            String newUser = String.valueOf(userField.getText());
            success = c.updateMetadataEntry(p,"username",newUser);
            
            if (success && username != null && oldUser != null && !newUser.equals(oldUser)){
            	kl.addUpdateKeychainLogEntry(username, true, keyId, 
            			String.format("Password %s changed username in keychain <%s>", p.getTitle(), keyName));
            }
            
            String oldComments = p.metadata.get("comments");
            String newComments = String.valueOf(comments.getText());
            success = c.updateMetadataEntry(p,"comments",newComments);
            
            if (success && username != null && !newComments.equals(oldComments)){
            	kl.addUpdateKeychainLogEntry(username, true, keyId, 
            			String.format("Password %s changed comments in keychain <%s>", p.getTitle(), keyName));
            }

            byte[] oldKey = p.password.getEncoded();
            String newKey = String.valueOf(passField.getPassword());
            success = c.updatePassword(p, Crypto.secretKeyFromBytes(newKey.getBytes(StandardCharsets.UTF_8)));
            
            if (success && username != null && !Arrays.areEqual(oldKey, newKey.getBytes(StandardCharsets.UTF_8))){
            	kl.addUpdateKeychainLogEntry(username, true, keyId, 
            			String.format("Password %s changed password value in keychain <%s>", p.getTitle(), keyName));
            }

            frame.dispose();
            kv.refreshTabbedPanes();
        });

    }


    private void createUIComponents(Password p) {
        // TODO: place custom component creation code here
        frame = new JFrame("Password");
        frame.setMinimumSize(new Dimension(300,400));

        Map<String,String> metadata = p.metadata;

        if (metadata.containsKey("title"))
            titleField.setText(metadata.get("title"));
        else
            titleField.setText("");

        if (metadata.containsKey("username"))
            userField.setText(metadata.get("username"));
        else
            userField.setText("");

        byte[] bytes = Crypto.secretKeyToBytes(p.password);
        passField.setText(new String(bytes, StandardCharsets.UTF_8));

        if (metadata.containsKey("comments"))
            comments.setText(metadata.get("comments"));
        else
            comments.setText("");

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        panel = new JPanel(gridbag);

        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        panel.setBorder(padding);

        double titleWeight = 0.1;
        double textWeight = 1;

        c.anchor =  GridBagConstraints.NORTHWEST;
        c.weightx = titleWeight;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth=1;
        JLabel titleLabel = new JLabel("Title");
        gridbag.setConstraints(titleLabel,c);
        panel.add(titleLabel);

        c.weightx = textWeight;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth=2;
        gridbag.setConstraints(titleField,c);
        panel.add(titleField);

        c.weightx = titleWeight;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth=1;
        JLabel userLabel = new JLabel("Username");
        gridbag.setConstraints(userLabel,c);
        panel.add(userLabel);

        c.weightx = textWeight;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth=2;
        gridbag.setConstraints(userField,c);
        panel.add(userField);

        c.weightx = titleWeight;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth=1;
        JLabel passLabel = new JLabel("Password");
        gridbag.setConstraints(passLabel,c);
        panel.add(passLabel);

        JPanel passPanel = new JPanel(new BorderLayout());
        passPanel.add(passField,BorderLayout.CENTER);
        passPanel.add(toggleView,BorderLayout.EAST);
        c.weightx = textWeight;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth=2;
        gridbag.setConstraints(passPanel,c);
        panel.add(passPanel);

        c.weightx = titleWeight;
        c.gridx = 0;
        c.gridy = 3;
        JLabel commentsLabel = new JLabel("Comments");
        gridbag.setConstraints(commentsLabel,c);
        panel.add(commentsLabel);

        c.weightx = textWeight;
        c.gridx = 1;
        c.gridy = 3;
        gridbag.setConstraints(comments,c);
        panel.add(comments);

        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 4;
        c.insets = new Insets(10,0,0,0);
        gridbag.setConstraints(saveButton,c);
        panel.add(saveButton);

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
