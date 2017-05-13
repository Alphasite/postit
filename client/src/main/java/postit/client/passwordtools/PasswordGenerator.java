package postit.client.passwordtools;

import javax.json.*;
import javax.swing.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jackielaw on 3/27/17.
 */
public class PasswordGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static final String CONFIGURATIONS = "configurations";

    private final static String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private final static String NUMBERS = "0123456789";

    public List<PasswordGeneratorConfiguration> configurations;
    PasswordGeneratorConfiguration activeConfiguration;

    public PasswordGenerator() {
        this.activeConfiguration = new PasswordGeneratorConfiguration();
        this.configurations = new ArrayList<>();
    }

    public PasswordGenerator(JsonObject object) {
        this.configurations = new ArrayList<>();

        JsonArray passwords = object.getJsonArray(CONFIGURATIONS);
        for (int i = 0; i < passwords.size(); i++) {
            PasswordGeneratorConfiguration config = new PasswordGeneratorConfiguration(passwords.getJsonObject(i));
            this.configurations.add(config);
        }

        if (this.configurations.size() > 0) {
            this.activeConfiguration = this.configurations.get(0);
        } else {
            this.activeConfiguration = new PasswordGeneratorConfiguration();
        }
    }

    public void editSettings(JFrame frame){
            SpinnerNumberModel model = new SpinnerNumberModel();
            model.setMaximum(256);
            model.setMinimum(8);

            JSpinner newLength = new JSpinner(model);
            newLength.setValue(activeConfiguration.passwordlength);

            JCheckBox upper = new JCheckBox();
            upper.setText("Upper case (A-Z)?");
            upper.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.activeConfiguration.useUpper)
                upper.setSelected(true);

            JCheckBox lower = new JCheckBox();
            lower.setText("Lower case (a-z)?");
            lower.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.activeConfiguration.useLower)
                lower.setSelected(true);

            JCheckBox numbers = new JCheckBox();
            numbers.setText("Number (0-9)?");
            numbers.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.activeConfiguration.useNumbers)
                numbers.setSelected(true);

            JCheckBox symbols = new JCheckBox();
            symbols.setText("Symbols?");
            symbols.setHorizontalTextPosition(SwingConstants.LEFT);
            if(this.activeConfiguration.useSymbols)
                symbols.setSelected(true);
            JTextField permittedSymbols = new JTextField(this.activeConfiguration.SYMBOLS);
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
                    this.activeConfiguration.passwordlength = (int) newLength.getValue();
                    this.activeConfiguration.useUpper = upper.isSelected();
                    this.activeConfiguration.useLower = lower.isSelected();
                    this.activeConfiguration.useNumbers = numbers.isSelected();
                    this.activeConfiguration.useSymbols = symbols.isSelected();
                    permittedSymbols.setText(removeDuplicates(permittedSymbols.getText()));
                    this.activeConfiguration.SYMBOLS=permittedSymbols.getText();
                }
                message.add("Some chars must be selected");

            } while (!(activeConfiguration.useUpper||activeConfiguration.useLower||activeConfiguration.useNumbers||activeConfiguration.useSymbols));
    }

    public String generatePassword(){
        StringBuilder password = new StringBuilder();

        String VALIDCHARS = "";

        if (this.activeConfiguration.useUpper){
            VALIDCHARS+=UPPER;
            int index = random.nextInt(UPPER.length());
            password.append(UPPER.charAt(index));
        }
        if (this.activeConfiguration.useLower){
            VALIDCHARS+=LOWER;
            int index = random.nextInt(LOWER.length());
            password.append(LOWER.charAt(index));
        }
        if (this.activeConfiguration.useNumbers){
            VALIDCHARS+=NUMBERS;
            int index = random.nextInt(NUMBERS.length());
            password.append(NUMBERS.charAt(index));
        }
        if (this.activeConfiguration.useSymbols){
            VALIDCHARS+=this.activeConfiguration.SYMBOLS;
            int index = random.nextInt(this.activeConfiguration.SYMBOLS.length());
            password.append(this.activeConfiguration.SYMBOLS.charAt(index));
        }
        while(this.activeConfiguration.passwordlength!=password.length()){
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

    public JsonObjectBuilder dump() {
        JsonArrayBuilder configurations = Json.createArrayBuilder();
        for (PasswordGeneratorConfiguration configuration : this.configurations) {
            configurations.add(configuration.dump());
        }

        return Json.createObjectBuilder()
                .add(CONFIGURATIONS, configurations);
    }
}
