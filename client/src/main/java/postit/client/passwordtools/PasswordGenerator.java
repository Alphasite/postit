package postit.client.passwordtools;

import javax.json.*;
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
    public PasswordGeneratorConfiguration activeConfiguration;

    public PasswordGenerator() {
        this.activeConfiguration = new PasswordGeneratorConfiguration();
        this.configurations = new ArrayList<>();
        configurations.add(activeConfiguration);
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
            configurations.add(activeConfiguration);
        }
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
        ArrayList<Character> passwordList = new ArrayList<>();
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

    public String removeDuplicates(String stringWithDups){
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
