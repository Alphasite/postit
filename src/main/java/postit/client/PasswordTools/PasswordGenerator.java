package postit.client.PasswordTools;

import javax.swing.*;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by jackielaw on 3/27/17.
 */
public class PasswordGenerator {

    private int passwordlength;
    private boolean useUpper;
    private boolean useLower;
    private boolean useNumbers;
    private boolean useSymbols;

    private static final SecureRandom random = new SecureRandom();

    private final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private final String NUMBERS = "0123456789";
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
            upper.setText("Upper case?");
            upper.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useUpper)
                upper.setSelected(true);

            JCheckBox lower = new JCheckBox();
            lower.setText("Lower case?");
            lower.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.useLower)
                lower.setSelected(true);

            JCheckBox numbers = new JCheckBox();
            numbers.setText("Number?");
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
                    this.SYMBOLS=permittedSymbols.getText();
                }
                message.add("Some chars must be selected");

            }while(!(useUpper||useLower||useNumbers||useSymbols));
    }

    public String generatePassword(){

        String VALIDCHARS = "";

        if (useUpper) VALIDCHARS+=UPPER;
        if (useLower) VALIDCHARS+=LOWER;
        if (useNumbers) VALIDCHARS+=NUMBERS;
        if (useSymbols) VALIDCHARS+=SYMBOLS;

        StringBuilder password = new StringBuilder();
        
        for(int i=0; i<passwordlength;i++) {
            int index = random.nextInt(VALIDCHARS.length());
            password.append(VALIDCHARS.charAt(index));
        }
        return password.toString();
    }
}
