package postit.shared;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Created by Zhan on 3/23/2017.
 */
public class Classify {

    public enum Level {
        HIGH,
        MEDIUM,
        LOW,
        ERROR
    }

    public static final String WEAK = "weak";
    public static final String STRONG = "strong";

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

    public Level strengthCheck (String password){
        //TODO: modify to get more levels of strength
        try {
            if ((comprehensive8(password, wordlist) && blacklistHard(password, wordlist2))
                    || basic16(password)){
                return Level.HIGH;
            }
            else
                return Level.LOW;
        } catch (Exception e) {
            e.printStackTrace();
            return Level.ERROR;
        }
    }

//    public static void main (String[] args) {
//        Path curr = Paths.get("");
//        File file = Paths.get("").resolve("src").resolve("main").resolve("resources").
//                resolve("passwordStrength").resolve("password-2011.lst").toFile();
//        System.out.println(curr.toAbsolutePath().toString());
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(file));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }
}
