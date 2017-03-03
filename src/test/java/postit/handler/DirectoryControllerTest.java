package postit.handler;

import postit.backend.*;
import postit.keychain.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * Created by nishadmathur on 2/3/17.
 */
public class DirectoryControllerTest {
    DirectoryEntry entry;
    SecretKey key;

    MockKeyService keyService;
    BackingStore backingStore;
    Directory directory;

    DirectoryController controller;

    @Before
    public void setUp() throws Exception {
        try {
            Crypto.init();

            keyService = new MockKeyService(Crypto.hashedSecretKeyFromBytes("DirectoryControllerTest".getBytes()));
            backingStore = new MockBackingStoreImpl(keyService);
            directory = new Directory(keyService, backingStore);
            controller = new DirectoryController(directory, keyService);

            backingStore.init();
        } catch (Exception e) {
            Files.deleteIfExists(backingStore.getDirectoryPath());
            FileUtils.deleteDirectory(backingStore.getKeychainsPath().toFile());
            throw e;
        }
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(backingStore.getDirectoryPath());
        FileUtils.deleteDirectory(backingStore.getKeychainsPath().toFile());
        Files.deleteIfExists(backingStore.getVolume());
    }

    @Test
    public void getKeychains() throws Exception {
        assertThat(controller.createKeychain("test1"), is(true));
        assertThat(controller.createKeychain("test2"), is(true));

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test1"), is(true));
        assertThat(names.contains("test2"), is(true));
    }

    @Test
    public void getKeychain() throws Exception {
        assertThat(controller.createKeychain("test3"), is(true));
        assertThat(controller.getKeychains().size(), is(1));

        Optional<Keychain> keychain = controller.getKeychain("test3");

        assertThat(keychain.isPresent(), is(true));
        assertThat(keychain.get().name, is("test3"));
    }

    @Test
    public void getKeychain1() throws Exception {
        assertThat(controller.createKeychain("test4"), is(true));
        assertThat(controller.getKeychains().size(), is(1));

        DirectoryEntry entry = controller.getKeychains().get(0);

        assertThat(entry, notNullValue());

        Optional<Keychain> keychain = entry.readKeychain();

        assertThat(keychain.isPresent(), is(true));
        assertThat(keychain.get().name, is("test4"));
    }

    @Test
    public void getPasswords() throws Exception {
        controller.createKeychain("test5");
        Keychain keychain = controller.getKeychain("test5").get();
        assertThat(controller.createPassword(keychain, "password1", Crypto.secretKeyFromBytes("secret1".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password2", Crypto.secretKeyFromBytes("secret2".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);
        List<String> passwordNames = passwords.stream()
                .map(password -> password.identifier)
                .collect(Collectors.toList());

        assertThat(passwords.size(), is(2));
        assertThat(passwordNames, hasItem("password1"));
        assertThat(passwordNames, hasItem("password2"));
    }

    @Test
    public void createKeychain() throws Exception {
        assertThat(controller.createKeychain("test6"), is(true));
        assertThat(controller.createKeychain("test7"), is(true));

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test6"), is(true));
        assertThat(names.contains("test7"), is(true));

        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test6.keychain")), is(true));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test7.keychain")), is(true));

        controller = new DirectoryController(backingStore.readDirectory().get(), keyService);

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test6"), is(true));
        assertThat(names.contains("test7"), is(true));

        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test6.keychain")), is(true));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test7.keychain")), is(true));
    }

    @Test
    public void createPassword() throws Exception {
        controller.createKeychain("test8");
        Keychain keychain = controller.getKeychain("test8").get();
        assertThat(controller.createPassword(keychain, "password3", Crypto.secretKeyFromBytes("secret3".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password4", Crypto.secretKeyFromBytes("secret4".getBytes())), is(true));

        List<Password> passwords = controller.getKeychain("test8").get().passwords;
        assertThat(passwords.size(), is(2));

        List<String> passwordStrings = passwords.stream()
                .map(key -> key.password)
                .map(Crypto::secretKeyToBytes)
                .map(String::new)
                .collect(Collectors.toList());

        assertThat(passwordStrings.contains("secret3"), is(true));
        assertThat(passwordStrings.contains("secret4"), is(true));

        controller = new DirectoryController(backingStore.readDirectory().get(), keyService);

        passwords = controller.getKeychain("test8").get().passwords;
        assertThat(passwords.size(), is(2));

        passwordStrings = passwords.stream()
                .map(key -> key.password)
                .map(Crypto::secretKeyToBytes)
                .map(String::new)
                .collect(Collectors.toList());

        assertThat(passwordStrings.contains("secret3"), is(true));
        assertThat(passwordStrings.contains("secret4"), is(true));
    }

    @Test
    public void getPassword() throws Exception {
        controller.createKeychain("test9");
        Keychain keychain = controller.getKeychain("test9").get();
        assertThat(controller.createPassword(keychain, "password5", Crypto.secretKeyFromBytes("secret5".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password6", Crypto.secretKeyFromBytes("secret6".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);
        List<String> passwordNames = passwords.stream()
                .map(password -> password.identifier)
                .collect(Collectors.toList());

        assertThat(passwords.size(), is(2));
        assertThat(passwordNames, hasItem("password5"));
        assertThat(passwordNames, hasItem("password6"));

        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }
    }

    @Test
    public void updatePassword() throws Exception {
        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.updatePassword(
                    password,
                    Crypto.secretKeyFromBytes(("secret" + (number + 2)).getBytes())
            ), is(true));
        }

        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number + 2));
        }

        controller = new DirectoryController(backingStore.readDirectory().get(), keyService);
        passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number + 2));
        }
    }

    @Test
    public void deleteKeychain() throws Exception {
        controller.createKeychain("test11");
        controller.createKeychain("test12");

        Keychain keychain11 = controller.getKeychain("test11").get();
        Keychain keychain12 = controller.getKeychain("test12").get();

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test11"), is(true));
        assertThat(names.contains("test12"), is(true));

        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test11.keychain")), is(true));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test12.keychain")), is(true));
        assertThat(controller.deleteKeychain(keychain11), is(true));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test11.keychain")), is(false));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test12.keychain")), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));

        controller = new DirectoryController(backingStore.readDirectory().get(), keyService);
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test11.keychain")), is(false));
        assertThat(Files.exists(backingStore.getKeychainsPath().resolve("test12.keychain")), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));
    }

    @Test
    public void deletePassword() throws Exception {
        controller.createKeychain("test13");
        Keychain keychain = controller.getKeychain("test13").get();
        assertThat(controller.createPassword(keychain, "password10", Crypto.secretKeyFromBytes("secret10".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password11", Crypto.secretKeyFromBytes("secret11".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password12", Crypto.secretKeyFromBytes("secret12".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(3));
        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        Password password11 = passwords.stream()
                .filter(password -> password.identifier.equals("password11"))
                .findAny()
                .get();

        assertThat(controller.deletePassword(password11), is(true));

        controller = new DirectoryController(backingStore.readDirectory().get(), keyService);
        passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(2));
        for (Password password : passwords) {
            String number = password.identifier.substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        List<String> names = passwords.stream()
                .map(password -> password.identifier)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names, hasItem("password10"));
        assertThat(names, hasItem("password12"));
    }

}