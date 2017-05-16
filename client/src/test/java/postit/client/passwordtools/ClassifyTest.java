package postit.client.passwordtools;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by nishadmathur on 12/5/17.
 */
public class ClassifyTest {
    @Test
    public void checkDicWords() throws Exception {
        assertThat(Classify.checkDicWords("banana", Classify.wordlist), is(true));
        assertThat(Classify.checkDicWords("passwordpassword", Classify.wordlist), is(false));
        assertThat(Classify.checkDicWords("passwordpassword123", Classify.wordlist), is(false));
    }

    @Test
    public void basic16() throws Exception {
        assertThat(Classify.basic16("asdasdasd"), is(false));
        assertThat(Classify.basic16("passwordpassword"), is(true));
    }

    @Test
    public void comprehensive8() throws Exception {
        assertThat(Classify.comprehensive8("asdasdasd", Classify.wordlist), is(false));
        assertThat(Classify.comprehensive8("passwordpassword", Classify.wordlist), is(false));
        assertThat(Classify.comprehensive8("!@baNadsfsd12", Classify.wordlist), is(true));
    }

    @Test
    public void blacklistHard() throws Exception {
        assertThat(Classify.blacklistHard("bananas", Classify.wordlist2), is(false));
        assertThat(Classify.blacklistHard("cake", Classify.wordlist2), is(false));
        assertThat(Classify.blacklistHard("sdjfhsdfahksd", Classify.wordlist2), is(true));
    }

    @Test
    public void strengthCheck() throws Exception {
        assertThat(new Classify().strengthCheck("banana").getString("evaluation").contains("dictionary"), is(true));
        assertThat(new Classify().strengthCheck("123Banana#$").getString("strength").contains("LOW"), is(true));
        assertThat(new Classify().strengthCheck("passwordpassword123Banana#$").getString("strength").contains("HIGH"), is(true));
    }

    @Test
    public void isWeak() throws Exception {
        assertThat(new Classify().isWeak("banana"), is(true));
        assertThat(new Classify().isWeak("123Banana#$"), is(true));
        assertThat(new Classify().isWeak("passwordpassword123Banana#$"), is(false));
    }

}