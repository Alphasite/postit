package cli;

import backend.Crypto;
import keychain.Directory;
import keychain.DirectoryEntry;
import keychain.Keychain;
import keychain.Password;

import javax.crypto.SecretKey;
import java.io.Console;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by nishadmathur on 22/2/17.
 */
public class CommandLineParser {
    public static void parse(String[] args, Directory directory) {
        if (args.length > 0 && args[0].equals("init")) {
            CommandLineParser.init(args);
        }

        if (args.length > 0 && args[0].equals("keychain")) {
            CommandLineParser.keychain(args, directory);
        }

        if (args.length > 0 && args[0].equals("password")) {
            CommandLineParser.password(args, directory);
        }

        if (args.length > 0 && args[0].equals("interactive")) {
            Scanner input = new Scanner(System.in);
            while (true) {
                System.out.print("\n> ");
                String[] line = input.nextLine().split("\\s+");

                if (line.length > 0 && line[0].equals("exit")) {
                    break;
                }

                CommandLineParser.parse(line, directory);
            }
        }
    }

    private static void init(String[] args) {

    }

    private static void keychain(String[] args, Directory directory) {
        if (args.length >= 2 && args[1].equals("create")) {
            if (args.length < 3) {
                System.err.println("Invalid arguments: 'postit keychain create <name>'");
            }

            if (args.length == 3) {
                directory.createKeychain(Crypto.generateKey(), args[2]);
            } else {
                Optional<DirectoryEntry> matchingKeychain = directory.getKeychains().stream()
                        .filter(entry -> entry.name.equals(args[2]))
                        .findAny();

                if (matchingKeychain.isPresent()) {
                    Optional<Keychain> keychain = matchingKeychain.get().readKeychain();

                    if (!keychain.isPresent()) {
                        // TODO
                    }

                    Optional<Password> password = keychain.get().passwords.stream()
                            .filter(p -> p.identifier.equals(args[3])).findAny();

                    if (password.isPresent()) {
                        // TODO
                    }

                    Console console = System.console();
                    String newPassword = new String(console.readPassword("Please enter master password: "));
                    SecretKey key = Crypto.secretKeyFromBytes(newPassword.getBytes());

                    keychain.get().passwords.add(new Password(args[3], key, keychain.get()));
                    keychain.get().save();
                }
            }
        }

        if (args.length >= 2 && args[1].equals("list")) {
            String keys = directory.getKeychains().stream()
                    .map(entry -> entry.name)
                    .collect(Collectors.joining("Keychains:\n\t","\t", "\n"));
            System.out.println(keys);
        }

        if (args.length >= 2 && args[1].equals("delete")) {
            if (args.length < 3) {
                System.err.println("Invalid arguments: 'postit keychain delete <name>'");
            }

            Optional<DirectoryEntry> matchingKeychain = directory.getKeychains().stream()
                    .filter(entry -> entry.name.equals(args[2]))
                    .findAny();

            if (matchingKeychain.isPresent()) {
                if (args.length == 3) {
                    matchingKeychain.get().delete();
                } else {
                    Optional<Keychain> keychain = matchingKeychain.get().readKeychain();

                    if (!keychain.isPresent()) {
                        // TODO
                    }

                    Optional<Password> password = keychain.get().passwords.stream().filter(p -> p.identifier.equals(args[3])).findAny();

                    if (!password.isPresent()) {
                        // TODO
                    }

                    password.get().delete();
                }
            } else {
                System.out.println("No matcing keychains found...");
            }
        }
    }

    private static void password(String[] args, Directory directory) {

    }
}
