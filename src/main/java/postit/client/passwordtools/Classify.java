package postit.client.passwordtools;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Zhan on 3/23/2017.
 */

public class Classify {

    final String dictionaryWords = "Contains dictionary words";
    final String comp8 = "Lacks upper case/lower case/symbol/digit";
    final String bas16 = "Longer password, at least 16 characters";

    public static class Result {
        private String evaluation;
        private String strength;

        public Result (String evaluation, Level strength) {
            this.evaluation = evaluation;
            this.strength = strength.name();
        }

        public String getEvaluation () {
            return evaluation;
        }

        public String getStrength () {
            return strength;
        }

        private JSONObject toObject () {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("evaluation", getEvaluation());
            jsonObject.put("strength", getStrength());

            return jsonObject;
        }
    }

    public enum Level {
        HIGH,
        MEDIUM,
        LOW,
        ERROR
    }

    File wordlist = Paths.get("").resolve("src").resolve("main").resolve("resources").
            resolve("passwordStrength").resolve("password-2011.lst").toFile();
    File wordlist2 = Paths.get("").resolve("src").resolve("main").resolve("resources").
            resolve("passwordStrength").resolve("words.txt").toFile();

    /**“Password must have at
     least 8 characters,
     an uppercase letter,
     lowercase letter,
     a symbol,
     a digit,
     It may not contain a dictionary word.”**/

    /** We removed
     non-alphabetic characters and checked the remainder against
     a dictionary, ignoring case.**/

    public static boolean checkDicWords (String target, File file) throws Exception{
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.indexOf(target.toLowerCase()) != -1) {
                    return true;
                }
            }
            in.close();
        } catch (IOException e) {
            // debug use
            e.printStackTrace();
        }
        return false;
    }


    public static boolean basic16 (String password){
        return password.length() >= 16;
    }

    public static boolean comprehensive8 (String password, File wordlist) throws Exception{
        String regex = "^(?=.*[A-Z])(?=.*[-!@#$&*])(?=.*[0-9])(?=.*[a-z]).{8,}$";
        boolean flag = password.matches(regex);
        if (flag) { // check if dictionary 8, i.e. contains a dictionary word
            String word = password.replaceAll("[^a-zA-Z]", "");
            if (!checkDicWords(word, wordlist)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blacklistHard (String password, File wordlist) throws Exception{
        return !checkDicWords(password, wordlist);
    }

    public JSONObject strengthCheck (String password){
        try {
            // basic16 is the highest strength, c8 and blacklist hard is medium,
            // everything else is low
            boolean c8 = comprehensive8(password, wordlist);
            boolean bListHard = blacklistHard(password, wordlist2);

            if (basic16(password)){
                return new Result("", Level.HIGH).toObject();
            }
            else if (c8 && bListHard) {
                return new Result(bas16, Level.MEDIUM).toObject();
            }
            else {
                if (!c8 && !bListHard) {
                    return new Result(bas16 + " " + comp8, Level.LOW).toObject();
                }
                if (!c8) {
                    return new Result(bas16 + " " + comp8, Level.LOW).toObject();
                }

                return new Result(bas16 + " " + dictionaryWords, Level.LOW).toObject();

            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(Level.ERROR.name(), Level.ERROR).toObject();
        }
    }

    public static void main (String[] args) {
        Path curr = Paths.get("");
        File file = Paths.get("").resolve("src").resolve("main").resolve("resources").
                resolve("passwordStrength").resolve("password-2011.lst").toFile();
        System.out.println(curr.toAbsolutePath().toString());
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
