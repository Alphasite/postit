package postit.client.passwordtools;

import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObjectBuilder;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by nishadmathur on 12/5/17.
 */
public class PasswordGeneratorTest {
    PasswordGenerator passwordGenerator;

    @Before
    public void setUp() throws Exception {
        passwordGenerator = new PasswordGenerator();
    }

    @Test
    public void generatePassword() throws Exception {
        String password = passwordGenerator.generatePassword()
                + passwordGenerator.generatePassword()
                + passwordGenerator.generatePassword()
                + passwordGenerator.generatePassword()
                + passwordGenerator.generatePassword()
                + passwordGenerator.generatePassword();

        assertThat(password.length(), is(passwordGenerator.activeConfiguration.passwordlength * 6));
        assertThat(password.chars().filter(c -> "abcdefghijklmnopqrstuvwxyz".contains("" + (char) c)).count(), not(is(0)));
        assertThat(password.chars().filter(c -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ".contains("" + (char) c)).count(), not(is(0)));
        assertThat(password.chars().filter(c -> "0123456789".contains("" + (char) c)).count(), not(is(0)));
    }

    @Test
    public void dumpLoad() throws Exception {
        passwordGenerator.activeConfiguration.SYMBOLS = "DEADBEEF";
        assertThat(passwordGenerator.configurations.size(), is(1));
        JsonObjectBuilder dump = passwordGenerator.dump();
        passwordGenerator = new PasswordGenerator(dump.build());
        assertThat(passwordGenerator.configurations.size(), is(1));
        assertThat(passwordGenerator.activeConfiguration.SYMBOLS.equals("DEADBEEF"), is(true));
    }
}