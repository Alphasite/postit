package postit.client.cli;

import postit.shared.Crypto;
import postit.client.backend.KeyService;
import postit.client.keychain.Directory;
import postit.client.keychain.DirectoryEntry;
import postit.client.keychain.Keychain;
import postit.client.keychain.Password;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

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

                CommandLineParser.parse(line, keyService, directory);
            }
        }
    }

    private static void init(String[] args) {

    }

    private static void keychain(String[] args, KeyService keyService, Directory directory) {
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

                    SecretKey key = keyService.getClientKey();

                    keychain.get().passwords.add(new Password(args[3], key, keychain.get()));
                }
            }
        }

        if (args.length >= 2 && args[1].equals("list")) {
            if (args.length == 2) {
                String keys = directory.getKeychains().stream()
                        .map(entry -> entry.name)
                        .collect(Collectors.joining("\t", "Keychains:\n\t","\n"));
                System.out.println(keys);
            } else {
                Optional<DirectoryEntry> matchingKeychain = directory.getKeychains().stream()
                        .filter(entry -> entry.name.equals(args[2]))
                        .findAny();

                if (!matchingKeychain.isPresent()) {
                    // TODO
                }

                Optional<Keychain> keychain = matchingKeychain.get().readKeychain();

                if (!keychain.isPresent()) {
                    // TODO
                }

                String keys = keychain.get().passwords.stream()
                        .map(password -> password.identifier + ": " + new String(Crypto.secretKeyToBytes(password.password)))
                        .collect(Collectors.joining("\n\t", "Keychains:\n\t", "\n"));
                System.out.println(keys);
            }
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
