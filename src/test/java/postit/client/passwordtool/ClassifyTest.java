package postit.client.passwordtool;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import postit.client.passwordtools.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by zhanzhao on 4/18/17.
 */
public class ClassifyTest {
    Classify classify;
    Classify.Level level;

    public final String dictionaryWords = "Contains dictionary/easy words";
    public final String comp8 = "Lacks upper case/lower case/symbol/digit";
    public final String bas16 = "Longer password, at least 16 characters";

    final String STRENGTH = "strength";
    final String EVALUATION = "evaluation";


    @Before
    public void setup () {
        classify = new Classify();
    }

    @Test
    public void testC8 () {
        String pwd1 = "12345678";
        JSONObject result1 = classify.strengthCheck(pwd1);
        assertEquals(Classify.Level.LOW.name(), result1.get(STRENGTH));
        assertEquals(bas16 + " " + comp8, result1.get(EVALUATION));

        String pwd2 = "1";
        JSONObject result2 = classify.strengthCheck(pwd2);
        assertEquals(Classify.Level.LOW.name(), result2.get(STRENGTH));
        assertEquals(bas16 + " " + dictionaryWords, result2.get(EVALUATION));

        String pwd3 = "Nushc-84";
        JSONObject result3 = classify.strengthCheck(pwd3);
        assertEquals(Classify.Level.MEDIUM.name(), result3.get(STRENGTH));
        assertEquals(bas16, result3.get(EVALUATION));

        String pwd4 = "Nushc-84ncwe";
        JSONObject result4 = classify.strengthCheck(pwd4);
        assertEquals(Classify.Level.MEDIUM.name(), result4.get(STRENGTH));
        assertEquals(bas16, result4.get(EVALUATION));
    }

    @Test
    public void test16 () {
        String pwd1 = "1234567812345678";
        JSONObject result1 = classify.strengthCheck(pwd1);
        assertEquals(Classify.Level.HIGH.name(), result1.get(STRENGTH));
        assertEquals("", result1.get(EVALUATION));

        String pwd2 = "1234567812345678jwn";
        JSONObject result2 = classify.strengthCheck(pwd2);
        assertEquals(Classify.Level.HIGH.name(), result2.get(STRENGTH));
        assertEquals("", result2.get(EVALUATION));
    }

    @Test
    public void testBlacklist () {
        String pwd1 = "lovelove";
        JSONObject result1 = classify.strengthCheck(pwd1);
        assertEquals(Classify.Level.LOW.name(), result1.get(STRENGTH));
        assertEquals(bas16 + " " + comp8, result1.get(EVALUATION));

        String pwd2 = "Lovef-84";
        JSONObject result2 = classify.strengthCheck(pwd2);
        assertEquals(Classify.Level.MEDIUM.name(), result2.get(STRENGTH));
        assertEquals(bas16, result2.get(EVALUATION));
    }
}
