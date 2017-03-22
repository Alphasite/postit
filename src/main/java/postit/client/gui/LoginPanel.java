package postit.client.gui;

import javax.swing.*;
import java.awt.*;


/**
 * Created by jackielaw on 3/16/17.
 */
public class LoginPanel extends JPanel {
    JLabel l_account, l_password, r_account, r_password1, r_password2, r_email, r_first, r_last;
    JTextField l_accountfield, r_accountfield, r_emailfield, r_firstfield, r_lastfield;
//    JButton l_submitbtn, r_submitbtn, clearbtn;
    JPasswordField l_passfield, r_pass1field, r_pass2field;


    public JTabbedPane tabbedPane = new JTabbedPane();


    public LoginPanel() {
        setPreferredSize(new Dimension(500,350));
        createUIComponents();

        setVisible(true);

        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    private void createUIComponents() {
        //Create login tab
        JPanel loginpanel = new JPanel();
        loginpanel.setLayout(null);
        loginpanel.setPreferredSize(new Dimension(500,350));
        createLoginPanel(loginpanel);
        tabbedPane.addTab("Login",loginpanel);

        //Create registration tab
        JPanel regispanel = new JPanel();
        regispanel.setLayout(null);
        regispanel.setPreferredSize(new Dimension(500,350));
        createRegistrationPanel(regispanel);
        tabbedPane.addTab("Registration",regispanel);

        add(tabbedPane);
    }

    private void createLoginPanel(JPanel l){
        l_account = new JLabel("Username:");
        l_password = new JLabel("Password:");
        l_accountfield = new JTextField();
        l_passfield = new JPasswordField();
//        l_submitbtn = new JButton("Submit");
//
//        l_submitbtn.addActionListener(this);

        l_account.setBounds(80, 70, 100, 30);
        l_password.setBounds(80, 110, 100, 30);
        l_passfield.setBounds(200, 110, 200, 30);
        l_accountfield.setBounds(200, 70, 200, 30);
//
//        l_submitbtn.setBounds(200, 150, 100, 30);


        l.add(l_account);
        l.add(l_password);
        l.add(l_accountfield);
        l.add(l_passfield);
//        l.add(l_submitbtn);
    }

    private void createRegistrationPanel(JPanel r){
        r_account = new JLabel("Username:");
        r_first = new JLabel("First Name:");
        r_last = new JLabel("Last Name:");
        r_email = new JLabel("Email-ID:");
        r_password1 = new JLabel("Create Password:");
        r_password2 = new JLabel("Confirm Password:");
        r_accountfield = new JTextField();
        r_firstfield = new JTextField();
        r_lastfield = new JTextField();
        r_emailfield = new JTextField();
        r_pass1field = new JPasswordField();
        r_pass2field = new JPasswordField();

//
//        r_submitbtn = new JButton("Submit");
//        clearbtn = new JButton("Clear");

        r_first.setBounds(80, 10, 150, 30);
        r_last.setBounds(80, 50, 150, 30);
        r_account.setBounds(80, 90, 150, 30);
        r_email.setBounds(80, 130, 150, 30);
        r_password1.setBounds(80, 170, 150, 30);
        r_password2.setBounds(80, 210, 150, 30);

        r_firstfield.setBounds(200, 10, 200, 30);
        r_lastfield.setBounds(200, 50, 200, 30);
        r_accountfield.setBounds(200, 90, 200, 30);
        r_emailfield.setBounds(200, 130, 200, 30);
        r_pass1field.setBounds(200, 170, 200, 30);
        r_pass2field.setBounds(200, 210, 200, 30);
//        r_submitbtn.setBounds(200, 250, 100, 30);
//        clearbtn.setBounds(300, 250, 100, 30);
//
//        r_submitbtn.addActionListener(this);
//        clearbtn.addActionListener(this);

        r.add(r_first);
        r.add(r_last);
        r.add(r_account);
        r.add(r_email);
        r.add(r_password1);
        r.add(r_password2);
        r.add(r_firstfield);
        r.add(r_lastfield);
        r.add(r_accountfield);
        r.add(r_emailfield);
        r.add(r_pass1field);
        r.add(r_pass2field);
//        r.add(r_submitbtn);
//        r.add(clearbtn);
    }
/*
    public void actionPerformed(ActionEvent e){
        if (e.getSource()==l_submitbtn){
            //submit
        }
        if (e.getSource()==r_submitbtn){
            if (! isValidEmailAddress(r_emailfield.getText()))
                JOptionPane.showMessageDialog(this,
                        "Please enter a proper email address",
                        "Email address error",
                        JOptionPane.ERROR_MESSAGE);
            else if(r_pass1field.getPassword()!=r_pass2field.getPassword())
                JOptionPane.showMessageDialog(this,
                        "Passwords do not match",
                        "Password error",
                        JOptionPane.ERROR_MESSAGE);


        }
        else{
            r_accountfield.setText("");
            r_emailfield.setText("");
            r_pass1field.setText("");
            r_pass2field.setText("");
        }
    }
*/
    public static void main(String[] args) {
        new LoginPanel();
    }

    public static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }
}
