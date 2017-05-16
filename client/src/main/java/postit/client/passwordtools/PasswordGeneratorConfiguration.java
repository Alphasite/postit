package postit.client.passwordtools;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Created by nishadmathur on 12/5/17.
 */
public class PasswordGeneratorConfiguration {
    public static final String NAME = "name";
    public static final String LENGTH = "length";
    public static final String USE_UPPER = "use-upper";
    public static final String USE_LOWER = "use-lower";
    public static final String USE_NUMBERS = "use-numbers";
    public static final String USE_SYMBOLS = "use-symbols";
    public static final String SYMBOLS1 = "symbols";
    public String name;
    public int passwordlength;
    public boolean useUpper;
    public boolean useLower;
    public boolean useNumbers;
    public boolean useSymbols;
    public String SYMBOLS = "!@#$%^&*_=+-/.?<>)";

    public PasswordGeneratorConfiguration() {
        name = "Default";
        passwordlength=8;
        useUpper=true;
        useLower=true;
        useNumbers=true;
        useSymbols=true;
    }

    public PasswordGeneratorConfiguration(JsonObject object) {
        name = object.getString(NAME);
        passwordlength = object.getInt(LENGTH);
        useUpper = object.getBoolean(USE_UPPER);
        useLower = object.getBoolean(USE_LOWER);
        useNumbers = object.getBoolean(USE_NUMBERS);
        useSymbols = object.getBoolean(USE_SYMBOLS);
        SYMBOLS = object.getString(SYMBOLS1);
    }

    public JsonObjectBuilder dump() {
        return Json.createObjectBuilder()
                .add(NAME, name)
                .add(LENGTH, passwordlength)
                .add(USE_UPPER, useUpper)
                .add(USE_LOWER, useLower)
                .add(USE_NUMBERS, useNumbers)
                .add(USE_SYMBOLS, useSymbols)
                .add(SYMBOLS1, SYMBOLS);
    }
}
