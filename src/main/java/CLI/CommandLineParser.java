package CLI;

import backend.keyService.KeyService;
import Keychain.Directory;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class CommandLineParser {
    public static void parse(String[] args, KeyService keyService, Directory directory) {
        if (args.length > 0 && args[0].equals("init")) {
            CommandLineParser.init(args);
        }

        if (args.length > 0 && args[0].equals("keychain")) {
            CommandLineParser.keychain(args, keyService, directory);
        }
    }

    private static void init(String[] args) {

    }

    private static void keychain(String[] args, KeyService keyService, Directory directory) {
        if (args[1].equals("create") && args.length >= 2) {
            if (args.length < 3) {
                System.err.println("Invalid arguments: 'postit keychain create <name>'");
            }

            directory.createKeychain(keyService.getKey(), args[2]);
        }
    }
}
