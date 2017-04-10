package postit.client.passwordtools;

import javax.swing.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by jackielaw on 3/27/17.
 */
public class PasswordGenerator{

    private int passwordlength;
    private boolean useUpper;
    private boolean useLower;
    private boolean useNumbers;
    private boolean useSymbols;

    private static final SecureRandom random = new SecureRandom();

    private final static String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private final static String NUMBERS = "0123456789";
    private String SYMBOLS = "!@#$%^&*_=+-/.?<>)";


    public PasswordGenerator(){

        passwordlength=8;
        useUpper=true;
        useLower=true;
        useNumbers=true;
        useSymbols=true;
    }

    public void editSettings(JFrame frame){
            SpinnerNumberModel model = new SpinnerNumberModel();
            model.setMaximum(256);
            model.setMinimum(8);

            JSpinner newLength = new JSpinner(model);
            newLength.setValue(passwordlength);

            JCheckBox upper = new JCheckBox();
            upper.setText("Upper case (A-Z)?");
            upper.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useUpper)
                upper.setSelected(true);

            JCheckBox lower = new JCheckBox();
            lower.setText("Lower case (a-z)?");
            lower.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useLower)
                lower.setSelected(true);

            JCheckBox numbers = new JCheckBox();
            numbers.setText("Number (0-9)?");
            numbers.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useNumbers)
                numbers.setSelected(true);

            JCheckBox symbols = new JCheckBox();
            symbols.setText("Symbols?");
            symbols.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useSymbols)
                symbols.setSelected(true);
            JTextField permittedSymbols = new JTextField(SYMBOLS);
            symbols.addActionListener(e->{
                if (symbols.isSelected())
                    permittedSymbols.setEnabled(true);
                else
                    permittedSymbols.setEnabled(false);
            });
            permittedSymbols.addActionListener(e->{
                permittedSymbols.setText(removeDuplicates(permittedSymbols.getText()));
            });


            ArrayList<Object>  message = new ArrayList<Object>();
            message.add("Length");
            message.add(newLength);
            message.add(upper);
            message.add(lower);
            message.add(numbers);
            message.add(symbols);
            message.add(permittedSymbols);
            do{
                int option = JOptionPane.showConfirmDialog(frame, message.toArray(), "Password Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (option == JOptionPane.OK_OPTION) {
                    this.passwordlength = (int) newLength.getValue();
                    this.useUpper = upper.isSelected();
                    this.useLower = lower.isSelected();
                    this.useNumbers = numbers.isSelected();
                    this.useSymbols = symbols.isSelected();
                    permittedSymbols.setText(removeDuplicates(permittedSymbols.getText()));
                    this.SYMBOLS=permittedSymbols.getText();
                }
                message.add("Some chars must be selected");

            }while(!(useUpper||useLower||useNumbers||useSymbols));
    }

    public String generatePassword(){
        StringBuilder password = new StringBuilder();

        String VALIDCHARS = "";

        if (useUpper){
            VALIDCHARS+=UPPER;
            int index = random.nextInt(UPPER.length());
            password.append(UPPER.charAt(index));
        }
        if (useLower){
            VALIDCHARS+=LOWER;
            int index = random.nextInt(LOWER.length());
            password.append(LOWER.charAt(index));
        }
        if (useNumbers){
            VALIDCHARS+=NUMBERS;
            int index = random.nextInt(NUMBERS.length());
            password.append(NUMBERS.charAt(index));
        }
        if (useSymbols){
            VALIDCHARS+=SYMBOLS;
            int index = random.nextInt(SYMBOLS.length());
            password.append(SYMBOLS.charAt(index));
        }
        while(passwordlength!=password.length()){
            int index = random.nextInt(VALIDCHARS.length());
            password.append(VALIDCHARS.charAt(index));
        }
        ArrayList<Character> passwordList = new ArrayList<Character>();
        for (char c : password.toString().toCharArray()){
            passwordList.add(c);
        }

        Collections.shuffle(passwordList,random);

        StringBuilder passwordReturn = new StringBuilder();
        for (char c:passwordList){
            passwordReturn.append(String.valueOf(c));
        }
        return passwordReturn.toString();
    }

    private String removeDuplicates(String stringWithDups){
        HashSet<Character> symbolSet = new HashSet<>();
        for (char c:stringWithDups.toCharArray()){
            symbolSet.add(c);
        }
        StringBuilder symbolString = new StringBuilder();
        for(char c:symbolSet){
            symbolString.append(String.valueOf(c));
        }
        return symbolString.toString();
    }
}
