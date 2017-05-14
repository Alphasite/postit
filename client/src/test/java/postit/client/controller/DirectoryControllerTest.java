package postit.client.controller;

import postit.client.backend.*;
import postit.client.keychain.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import postit.shared.Crypto;

import javax.crypto.SecretKey;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * Created by nishadmathur on 2/3/17.
 *
 * Test class for directory controller.
 */
@SuppressWarnings({"Duplicates", "OptionalGetWithoutIsPresent", "ConstantConditions"})
public class DirectoryControllerTest {
    private final static Logger LOGGER = Logger.getLogger(DirectoryControllerTest.class.getName());

    DirectoryEntry entry;
    SecretKey key;

    MockKeyService keyService;
    MockBackingStore backingStore;
    Directory directory;

    DirectoryController controller;

    Account account;
    Share ownerShare;
    RSAPublicKey encryptionKey;
    RSAPublicKey signgingKey;

    @Before
    public void setUp() throws Exception {
        LOGGER.info("----Setup");

        try {
            Crypto.init(false);

            keyService = new MockKeyService(Crypto.secretKeyFromBytes("DirectoryControllerTest".getBytes()), null);
            keyService.account = new Account("test", "password");

            backingStore = new MockBackingStore(keyService);
            backingStore.init();

            directory = backingStore.readDirectory().get();
            controller = new DirectoryController(directory, backingStore, keyService);

            account = keyService.getAccount();
            encryptionKey = (RSAPublicKey) account.getEncryptionKeypair().getPublic();
            signgingKey = (RSAPublicKey) account.getSigningKeypair().getPublic();
            ownerShare = new Share(-1L, account.getUsername(), true, encryptionKey, signgingKey, true);
        } catch (Exception e) {
            Files.deleteIfExists(backingStore.getContainer());
            throw e;
        }

        LOGGER.info(backingStore.getVolume().toString());
    }

    @After
    public void tearDown() throws Exception {
        LOGGER.info("----Tear down");

        Files.deleteIfExists(backingStore.getContainer());
        Files.deleteIfExists(backingStore.getVolume());
    }

    @Test
    public void getKeychains() throws Exception {
        LOGGER.info("----getKeychains");

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
        LOGGER.info("----getKeychain");

        assertThat(controller.createKeychain("test3"), is(true));

        LOGGER.info("created keychain");

        assertThat(controller.getKeychains().size(), is(1));

        LOGGER.info("Got keychain");

        Optional<Keychain> keychain = controller.getKeychain("test3");

        LOGGER.info("Check keychain");

        assertThat(keychain.isPresent(), is(true));
    }

    @Test
    public void getKeychain1() throws Exception {
        LOGGER.info("----getKeychain1");

        assertThat(controller.createKeychain("test4"), is(true));

        System.err.println("created keychain");

        assertThat(controller.getKeychains().size(), is(1));

        DirectoryEntry entry = controller.getKeychains().get(0);

        System.err.println("Got keychain");

        assertThat(entry, notNullValue());

        Optional<Keychain> keychain = entry.readKeychain();

        System.err.println("Read keychain");

        assertThat(keychain.isPresent(), is(true));
//        assertThat(keychain.get().name, is("test4"));

        System.err.println("done");
    }

    @Test
    public void getPasswords() throws Exception {
        LOGGER.info("----getPasswords");

        controller.createKeychain("test5");
        Keychain keychain = controller.getKeychain("test5").get();
        assertThat(controller.createPassword(keychain, "password1", "testuser", Crypto.secretKeyFromBytes("secret1".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password2", "testuser", Crypto.secretKeyFromBytes("secret2".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);
        List<String> passwordNames = passwords.stream()
                .map(password -> password.getTitle())
                .collect(Collectors.toList());

        assertThat(passwords.size(), is(2));
        assertThat(passwordNames, hasItem("password1"));
        assertThat(passwordNames, hasItem("password2"));
    }

    @Test
    public void createKeychain() throws Exception {
        LOGGER.info("----createKeychain");

        assertThat(controller.createKeychain("test6"), is(true));
        assertThat(controller.createKeychain("test7"), is(true));

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test6"), is(true));
        assertThat(names.contains("test7"), is(true));

        assertThat(keychainExists("test6"), is(true));
        assertThat(keychainExists("test7"), is(true));
    }

    @Test
    public void createKeychainDuplicate() throws Exception {
        LOGGER.info("----createKeychain");

        assertThat(controller.createKeychain("test6"), is(true));
        assertThat(controller.createKeychain("test6"), is(false));

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test6"), is(true));

        assertThat(keychainExists("test6"), is(true));
    }

    @Test
    public void createKeychainPersistent() throws Exception {
        LOGGER.info("----createKeychain");

        assertThat(controller.createKeychain("test6"), is(true));
        assertThat(controller.createKeychain("test7"), is(true));

        reloadPersistent();

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test6"), is(true));
        assertThat(names.contains("test7"), is(true));

        assertThat(keychainExists("test6"), is(true));
        assertThat(keychainExists("test7"), is(true));
    }

    @Test
    public void createPassword() throws Exception {
        LOGGER.info("----createPassword");

        controller.createKeychain("test8");
        Keychain keychain = controller.getKeychain("test8").get();
        assertThat(controller.createPassword(keychain, "password3", "testuser", Crypto.secretKeyFromBytes("secret3".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password4", "testuser", Crypto.secretKeyFromBytes("secret4".getBytes())), is(true));

        List<Password> passwords = controller.getKeychain("test8").get().passwords;
        assertThat(passwords.size(), is(2));

        List<String> passwordStrings = passwords.stream()
                .map(key -> key.password)
                .map(Crypto::secretKeyToBytes)
                .map(String::new)
                .collect(Collectors.toList());

        assertThat(passwordStrings.contains("secret3"), is(true));
        assertThat(passwordStrings.contains("secret4"), is(true));
    }

    @Test
    public void createPasswordPersistent() throws Exception {
        LOGGER.info("----createPassword");

        controller.createKeychain("test8");
        Keychain keychain = controller.getKeychain("test8").get();
        assertThat(controller.createPassword(keychain, "password3", "testuser", Crypto.secretKeyFromBytes("secret3".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password4", "testuser", Crypto.secretKeyFromBytes("secret4".getBytes())), is(true));

        reloadPersistent();

        List<Password> passwords = controller.getKeychain("test8").get().passwords;
        assertThat(passwords.size(), is(2));

        List<String> passwordStrings = passwords.stream()
                .map(key -> key.password)
                .map(Crypto::secretKeyToBytes)
                .map(String::new)
                .collect(Collectors.toList());

        assertThat(passwordStrings.contains("secret3"), is(true));
        assertThat(passwordStrings.contains("secret4"), is(true));
    }

    @Test
    public void getPassword() throws Exception {
        LOGGER.info("----getPassword");

        controller.createKeychain("test9");
        Keychain keychain = controller.getKeychain("test9").get();
        assertThat(controller.createPassword(keychain, "password5", "testuser", Crypto.secretKeyFromBytes("secret5".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password6", "testuser", Crypto.secretKeyFromBytes("secret6".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);
        List<String> passwordNames = passwords.stream()
                .map(password -> password.getTitle())
                .collect(Collectors.toList());

        assertThat(passwords.size(), is(2));
        assertThat(passwordNames, hasItem("password5"));
        assertThat(passwordNames, hasItem("password6"));

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }
    }

    @Test
    public void updatePassword() throws Exception {
        LOGGER.info("----updatePassword");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser", Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser", Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.updatePassword(
                    password,
                    Crypto.secretKeyFromBytes(("secret" + (number + 2)).getBytes())
            ), is(true));
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number + 2));
        }
    }

    @Test
    public void updatePasswordPersistent() throws Exception {
        LOGGER.info("----updatePassword");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser", Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser", Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.updatePassword(
                    password,
                    Crypto.secretKeyFromBytes(("secret" + (number + 2)).getBytes())
            ), is(true));
        }

        reloadPersistent();
        passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number + 2));
        }
    }

    @Test
    public void deleteKeychain() throws Exception {
        LOGGER.info("----deleteKeychain");

        controller.createKeychain("test11");
        controller.createKeychain("test12");

        Keychain keychain11 = controller.getKeychain("test11").get();

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test11"), is(true));
        assertThat(names.contains("test12"), is(true));

        assertThat(keychainExists("test11"), is(true));
        assertThat(keychainExists("test12"), is(true));
        assertThat(controller.deleteKeychain(keychain11), is(true));
        assertThat(keychainExists("test11"), is(false));
        assertThat(keychainExists("test12"), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));
    }

    @Test
    public void deleteKeychainPersistent() throws Exception {
        LOGGER.info("----deleteKeychain");

        controller.createKeychain("test11");
        controller.createKeychain("test12");

        Keychain keychain11 = controller.getKeychain("test11").get();

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test11"), is(true));
        assertThat(names.contains("test12"), is(true));

        assertThat(keychainExists("test11"), is(true));
        assertThat(keychainExists("test12"), is(true));
        assertThat(controller.deleteKeychain(keychain11), is(true));

        reloadPersistent();

        assertThat(keychainExists("test11"), is(false));
        assertThat(keychainExists("test12"), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));
    }

    @Test
    public void deletePassword() throws Exception {
        LOGGER.info("----deletePassword");

        controller.createKeychain("test13");
        Keychain keychain = controller.getKeychain("test13").get();
        assertThat(controller.createPassword(keychain, "password10", "testuser", Crypto.secretKeyFromBytes("secret10".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password11", "testuser", Crypto.secretKeyFromBytes("secret11".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password12", "testuser", Crypto.secretKeyFromBytes("secret12".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(3));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        Password password11 = passwords.stream()
                .filter(password -> password.getTitle().equals("password11"))
                .findAny()
                .get();

        assertThat(controller.deletePassword(password11), is(true));

        passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(2));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        List<String> names = passwords.stream()
                .map(password -> password.getTitle())
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names, hasItem("password10"));
        assertThat(names, hasItem("password12"));
    }

    @Test
    public void deletePasswordPersistent() throws Exception {
        LOGGER.info("----deletePassword");

        controller.createKeychain("test13");
        Keychain keychain = controller.getKeychain("test13").get();
        assertThat(controller.createPassword(keychain, "password10", "testuser", Crypto.secretKeyFromBytes("secret10".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password11", "testuser", Crypto.secretKeyFromBytes("secret11".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password12", "testuser", Crypto.secretKeyFromBytes("secret12".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(3));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        Password password11 = passwords.stream()
                .filter(password -> password.getTitle().equals("password11"))
                .findAny()
                .get();

        assertThat(controller.deletePassword(password11), is(true));

        reloadPersistent();
        passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(2));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        List<String> names = passwords.stream()
                .map(Password::getTitle)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names, hasItem("password10"));
        assertThat(names, hasItem("password12"));
    }

    @Test
    public void getDeletedKeychains() throws Exception {
        LOGGER.info("----deleteKeychain");

        controller.createKeychain("test11");
        controller.createKeychain("test12");

        Keychain keychain11 = controller.getKeychain("test11").get();

        DirectoryEntry entry1 = controller.getKeychains().get(0);
        DirectoryEntry entry2 = controller.getKeychains().get(1);
        entry1.setServerid(1L);
        entry2.setServerid(2L);

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test11"), is(true));
        assertThat(names.contains("test12"), is(true));

        assertThat(controller.deleteKeychain(keychain11), is(true));
        assertThat(keychainExists("test11"), is(false));
        assertThat(keychainExists("test12"), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));

        assertThat(controller.getDeletedKeychains().contains(1L), is(true));
        assertThat(controller.getDeletedKeychains().contains(2L), is(false));
    }

    @Test
    public void getDeletedKeychainsPersistant() throws Exception {
        LOGGER.info("----getDeletedKeychainsPersistant");

        controller.createKeychain("test11");
        controller.createKeychain("test12");

        Keychain keychain11 = controller.getKeychain("test11").get();

        DirectoryEntry entry1 = controller.getKeychains().get(0);
        DirectoryEntry entry2 = controller.getKeychains().get(1);
        entry1.setServerid(1L);
        entry2.setServerid(2L);

        List<String> names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(2));
        assertThat(names.contains("test11"), is(true));
        assertThat(names.contains("test12"), is(true));

        assertThat(controller.deleteKeychain(keychain11), is(true));

        reloadPersistent();

        assertThat(keychainExists("test11"), is(false));
        assertThat(keychainExists("test12"), is(true));

        names = controller.getKeychains().stream()
                .map(entry -> entry.name)
                .collect(Collectors.toList());

        assertThat(names.size(), is(1));
        assertThat(names.contains("test11"), is(false));
        assertThat(names.contains("test12"), is(true));

        assertThat(controller.getDeletedKeychains().contains(1L), is(true));
        assertThat(controller.getDeletedKeychains().contains(2L), is(false));
    }

    @Test
    public void updateLocalIfIsOlder() throws Exception {
        controller.createKeychain("old");

        SecretKey expectedPasswordForTest = Crypto.secretKeyFromBytes("old".getBytes());
        controller.createPassword(
                controller.getKeychain("old").get(),
                "test",
                "testuser",
                expectedPasswordForTest
        );

        controller.createPassword(
                controller.getKeychain("old").get(),
                "json",
                "testuser",
                Crypto.secretKeyFromBytes("DEADBEEF".getBytes())
        );

        Password password1 = controller.getPasswords(controller.getKeychain("old").get())
                .stream()
                .filter(password -> password.getTitle().equals("json"))
                .findAny()
                .get();

        DirectoryEntry entry1 = new DirectoryEntry(
                "json",
                Crypto.secretKeyFromBytes("json".getBytes()),
                directory,
                backingStore,
                encryptionKey,
                signgingKey
        );

        entry1.setNonce(new byte[16]);
        entry1.setOwner(ownerShare.username);
        entry1.setServerid(5L);

        SecretKey expectedPasswordForJson = Crypto.secretKeyFromBytes("json".getBytes());
        Keychain keychain1 = new Keychain(entry1);
        Password passwordToMerge = new Password(password1.uuid, expectedPasswordForJson, keychain1);
        passwordToMerge.metadata.put("title", "json");
        keychain1.passwords.add(passwordToMerge);

        DirectoryEntry entry = controller.getKeychains().get(0);

        assertThat(controller.updateLocalIfIsOlder(
                entry,
                entry1.dump().build(),
                keychain1.dump().build(),
                ownerShare.username,
                true
        ), is(true));

        assertThat(controller.getKeychains().size(), is(1));
        DirectoryEntry oldEntry = controller.getKeychains().get(0);
        Keychain oldKeychain = controller.getKeychain("json").get();
        assertThat(oldKeychain, notNullValue());
        assertThat(oldKeychain.getName(), is("json"));
        assertThat(oldEntry.lastModified, is(entry1.lastModified));
        assertThat(oldEntry.getServerId(), is(-1L));
        assertThat(oldKeychain.passwords.size(), is(2));
        for (Password password : oldKeychain.passwords) {
            assertThat(password.getTitle(), anyOf(is("json"), is("test")));
            switch (password.getTitle()) {
                case "json":
                    assertThat(password.getPasswordAsString(), equalTo(new String(Crypto.secretKeyToBytes(expectedPasswordForJson))));
                    break;
                case "test":
                    assertThat(password.getPasswordAsString(), equalTo(new String(Crypto.secretKeyToBytes(expectedPasswordForTest))));
                    break;
            }

        }

    }

    @Test
    public void updateLocalIfIsOlderPersistent() throws Exception {
        controller.createKeychain("old");

        SecretKey expectedPasswordForTest = Crypto.secretKeyFromBytes("old".getBytes());
        controller.createPassword(
                controller.getKeychain("old").get(),
                "test",
                "testuser",
                expectedPasswordForTest
        );

        controller.createPassword(
                controller.getKeychain("old").get(),
                "json",
                "testuser",
                Crypto.secretKeyFromBytes("DEADBEEF".getBytes())
        );

        Password password1 = controller.getPasswords(controller.getKeychain("old").get())
                .stream()
                .filter(password -> password.getTitle().equals("json"))
                .findAny()
                .get();

        DirectoryEntry entry1 = new DirectoryEntry(
                "json",
                Crypto.secretKeyFromBytes("json".getBytes()),
                directory,
                backingStore,
                encryptionKey,
                signgingKey
        );

        entry1.setNonce(new byte[16]);
        entry1.setOwner(ownerShare.username);
        entry1.setServerid(5L);

        SecretKey expectedPasswordForJson = Crypto.secretKeyFromBytes("json".getBytes());
        Keychain keychain1 = new Keychain(entry1);
        Password passwordToMerge = new Password(password1.uuid, expectedPasswordForJson, keychain1);
        passwordToMerge.metadata.put("title", "json");
        keychain1.passwords.add(passwordToMerge);

        DirectoryEntry entry = controller.getKeychains().get(0);

        assertThat(controller.updateLocalIfIsOlder(
                entry,
                entry1.dump().build(),
                keychain1.dump().build(),
                ownerShare.username,
                true
        ), is(true));

        reloadPersistent();

        assertThat(controller.getKeychains().size(), is(1));
        DirectoryEntry oldEntry = controller.getKeychains().get(0);
        Keychain oldKeychain = controller.getKeychain("json").get();
        assertThat(oldKeychain, notNullValue());
        assertThat(oldKeychain.getName(), is("json"));
        assertThat(oldEntry.lastModified, is(entry1.lastModified));
        assertThat(oldEntry.getServerId(), is(5L));
        assertThat(oldKeychain.passwords.size(), is(2));
        for (Password password : oldKeychain.passwords) {
            assertThat(password.getTitle(), anyOf(is("json"), is("test")));
            switch (password.getTitle()) {
                case "json":
                    assertThat(password.getPasswordAsString(), equalTo(new String(Crypto.secretKeyToBytes(expectedPasswordForJson))));
                    break;
                case "test":
                    assertThat(password.getPasswordAsString(), equalTo(new String(Crypto.secretKeyToBytes(expectedPasswordForTest))));
                    break;
            }

        }
    }

    @Test
    public void createKeychain3() throws Exception {
        DirectoryEntry entry1 = new DirectoryEntry(
                "json",
                Crypto.secretKeyFromBytes("json".getBytes()),
                directory,
                backingStore,
                encryptionKey,
                signgingKey
        );

        entry1.setNonce(new byte[16]);
        entry1.setOwner(ownerShare.username);
        entry1.setServerid(5L);

        Keychain keychain1 = new Keychain(entry1);
        Password password = new Password("json", Crypto.secretKeyFromBytes("json".getBytes()), keychain1);
        password.metadata.put("title", "json2");
        keychain1.passwords.add(password);

        assertThat(controller.createKeychain(
                entry1.getServerId(),
                entry1.dump().build(),
                keychain1.dump().build()
        ), is(true));

        assertThat(controller.getKeychains().size(), is(1));
        DirectoryEntry oldEntry = controller.getKeychains().get(0);
        Keychain oldKeychain = controller.getKeychain("json").get();
        assertThat(oldKeychain, notNullValue());
        assertThat(oldKeychain.getName(), is("json"));
        assertThat(oldEntry.lastModified, is(entry1.lastModified));
        assertThat(oldEntry.getServerId(), is(5L));
        assertThat(oldKeychain.passwords.size(), is(1));
        assertThat(oldKeychain.passwords.get(0).uuid, is("json"));
        assertThat(oldKeychain.passwords.get(0).getTitle(), is("json2"));
        assertThat(oldKeychain.passwords.get(0).getPasswordAsString(), is("json"));
    }

    @Test
    public void createKeychain3Persistent() throws Exception {
        DirectoryEntry entry1 = new DirectoryEntry(
                "json",
                Crypto.secretKeyFromBytes("json".getBytes()),
                directory,
                backingStore,
                encryptionKey,
                signgingKey
        );

        entry1.setNonce(new byte[16]);
        entry1.setOwner(ownerShare.username);
        entry1.setServerid(5L);

        Keychain keychain1 = new Keychain(entry1);
        Password password = new Password("json", Crypto.secretKeyFromBytes("json".getBytes()), keychain1);
        password.metadata.put("title", "json2");
        keychain1.passwords.add(password);

        assertThat(controller.createKeychain(
                entry1.getServerId(),
                entry1.dump().build(),
                keychain1.dump().build()
        ), is(true));

        reloadPersistent();

        assertThat(controller.getKeychains().size(), is(1));
        DirectoryEntry oldEntry = controller.getKeychains().get(0);
        Keychain oldKeychain = controller.getKeychain("json").get();
        assertThat(oldKeychain, notNullValue());
        assertThat(oldKeychain.getName(), is("json"));
        assertThat(oldEntry.lastModified, is(entry1.lastModified));
        assertThat(oldEntry.getServerId(), is(5L));
        assertThat(oldKeychain.passwords.size(), is(1));
        assertThat(oldKeychain.passwords.get(0).uuid, is("json"));
        assertThat(oldKeychain.passwords.get(0).getTitle(), is("json2"));
        assertThat(oldKeychain.passwords.get(0).getPasswordAsString(), is("json"));
    }

    @Test
    public void updateMetadataCreate() throws Exception {
        LOGGER.info("----updateMetadataCreate");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser",Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser",Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "7")) {
                controller.updateMetadataEntry(password, "test", "banana");
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.get("test"), is("banana"));
            }
        }
    }

    @Test
    public void updateMetadataCreatePersistent() throws Exception {
        LOGGER.info("----updateMetadataCreatePersistent");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser",Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser",Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "7")) {
                controller.updateMetadataEntry(password, "test", "banana");
            }
        }

        reloadPersistent();

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.get("test"), is("banana"));
            }
        }
    }

    @Test
    public void updateMetadataUpdate() throws Exception {
        LOGGER.info("----updateMetadataUpdate");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser",Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser",Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "7")) {
                controller.updateMetadataEntry(password, "test", "banana");
                controller.updateMetadataEntry(password, "test2", "asdasd");
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test"), is("banana"));
                assertThat(password.metadata.get("test2"), is("asdasd"));
                assertThat(controller.updateMetadataEntry(password, "test", "ham"), is(true));
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test"), is("ham"));
                assertThat(password.metadata.get("test2"), is("asdasd"));
            }
        }
    }

    @Test
    public void updateMetadataUpdatePersistent() throws Exception {
        LOGGER.info("----updateMetadataUpdatePersistent");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser",Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser",Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "7")) {
                controller.updateMetadataEntry(password, "test", "banana");
                controller.updateMetadataEntry(password, "test2", "asdasd");
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test"), is("banana"));
                assertThat(password.metadata.get("test2"), is("asdasd"));
                assertThat(controller.updateMetadataEntry(password, "test", "ham"), is(true));
            }
        }

        reloadPersistent();

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(true));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test"), is("ham"));
                assertThat(password.metadata.get("test2"), is("asdasd"));
            }
        }
    }

    @Test
    public void deletePasswordMetadata() throws Exception {
        LOGGER.info("----deletePasswordMetadata");

        controller.createKeychain("test13");
        Keychain keychain = controller.getKeychain("test13").get();
        assertThat(controller.createPassword(keychain, "password10", "testuser",Crypto.secretKeyFromBytes("secret10".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password11", "testuser",Crypto.secretKeyFromBytes("secret11".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password12", "testuser",Crypto.secretKeyFromBytes("secret12".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(3));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        Password password11 = passwords.stream()
                .filter(password -> password.getTitle().equals("password11"))
                .findAny()
                .get();

        assertThat(controller.deletePassword(password11), is(true));

        passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(2));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "10")) {
                controller.updateMetadataEntry(password, "test", "banana");
                controller.updateMetadataEntry(password, "test2", "asdasd");
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "10")) {
                assertThat(controller.removeMetadataEntryIfExists(password, "test"), is(true));
            }
        }

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(false));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test2"), is("asdasd"));
            }
        }
    }

    @Test
    public void deletePasswordMetadataPersistent() throws Exception {
        LOGGER.info("----deletePasswordMetadata");

        controller.createKeychain("test13");
        Keychain keychain = controller.getKeychain("test13").get();
        assertThat(controller.createPassword(keychain, "password10", "testuser", Crypto.secretKeyFromBytes("secret10".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password11", "testuser", Crypto.secretKeyFromBytes("secret11".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password12", "testuser", Crypto.secretKeyFromBytes("secret12".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(3));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            assertThat(controller.getPassword(password), is("secret" + number));
        }

        Password password11 = passwords.stream()
                .filter(password -> password.getTitle().equals("password11"))
                .findAny()
                .get();

        assertThat(controller.deletePassword(password11), is(true));

        passwords = controller.getPasswords(keychain);

        assertThat(passwords.size(), is(2));
        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "10")) {
                controller.updateMetadataEntry(password, "test", "banana");
                controller.updateMetadataEntry(password, "test2", "asdasd");
            }
        }

        reloadPersistent();

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);

            if (Objects.equals(number, "10")) {
                assertThat(controller.removeMetadataEntryIfExists(password, "test"), is(true));
            }
        }

        reloadPersistent();

        for (Password password : passwords) {
            String number = password.getTitle().substring(8);
            if (Objects.equals(number, "7")) {
                assertThat(password.metadata.containsKey("test"), is(false));
                assertThat(password.metadata.containsKey("test2"), is(true));
                assertThat(password.metadata.get("test2"), is("asdasd"));
            }
        }
    }

    @Test
    public void getAccount() throws Exception {
        LOGGER.info("----getAccount");

        assertThat(controller.getAccount(), notNullValue());

        assertThat(controller.getAccount().get().getUsername(), is("test"));
        assertThat(controller.getAccount().get().getSecretKey(), notNullValue());
    }

    @Test
    public void getAccountPersistent() throws Exception {
        LOGGER.info("----getAccountPersistent");

        reloadPersistent();

        assertThat(controller.getAccount(), notNullValue());

        assertThat(controller.getAccount().get().getUsername(), is("test"));
        assertThat(controller.getAccount().get().getSecretKey(), notNullValue());
    }

    @Test
    public void buildKeychainEntryObject() throws Exception {

    }

    @Test
    public void setKeychainOnlineId() throws Exception {
            LOGGER.info("----setKeychainOnlineId");

            assertThat(controller.createKeychain("test3"), is(true));

            assertThat(controller.getKeychains().size(), is(1));

            assertThat(controller.setKeychainOnlineId(controller.getKeychains().get(0), 100), is(true));

            assertThat(controller.getKeychains().get(0).getServerId(), is(100L));
    }

    @Test
    public void setKeychainOnlineIdPersistent() throws Exception {
        LOGGER.info("----setKeychainOnlineId");

        assertThat(controller.createKeychain("test3"), is(true));

        assertThat(controller.getKeychains().size(), is(1));

        assertThat(controller.setKeychainOnlineId(controller.getKeychains().get(0), 100L), is(true));

        reloadPersistent();

        assertThat(controller.getKeychains().get(0).getServerId(), is(100L));
    }

    @Test
    public void updatePasswordTitle() throws Exception {
        LOGGER.info("----updatePasswordTitle");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser", Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser", Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            if (password.getTitle().equals("password8")) {
                controller.updatePasswordTitle(password, "banana");
            }
        }

        passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            assertThat(password.getTitle(), anyOf(is("banana"), is("password7")));
        }
    }

    @Test
    public void updatePasswordTitlePersistent() throws Exception {
        LOGGER.info("----updatePasswordTitlePersistent");

        controller.createKeychain("test10");
        Keychain keychain = controller.getKeychain("test10").get();
        assertThat(controller.createPassword(keychain, "password7", "testuser", Crypto.secretKeyFromBytes("secret7".getBytes())), is(true));
        assertThat(controller.createPassword(keychain, "password8", "testuser", Crypto.secretKeyFromBytes("secret8".getBytes())), is(true));

        List<Password> passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            if (password.getTitle().equals("password8")) {
                controller.updatePasswordTitle(password, "banana");
            }
        }

        reloadPersistent();

        passwords = controller.getPasswords(keychain);

        for (Password password : passwords) {
            assertThat(password.getTitle(), anyOf(is("banana"), is("password7")));
        }
    }

    private boolean keychainExists(String name) {
        JsonObject container = Crypto.readJsonObjectFromFile(backingStore.getContainer()).get();
        return container.getJsonObject("keychains").containsKey(name);
    }

    private void reloadPersistent() throws IOException {
        backingStore = backingStore.freshBackingStore();
        controller = new DirectoryController(backingStore.readDirectory().get(), backingStore, keyService);
    }
}