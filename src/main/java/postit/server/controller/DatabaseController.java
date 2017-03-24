package postit.server.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import postit.server.database.Database;
import postit.server.model.*;
import postit.shared.model.Account;

@SuppressWarnings("FieldCanBeLocal")
public class DatabaseController {

    private static final String ACCOUNT = "account";
    private static final String DIRECTORY = "directory";
    private static final String DIRECTORY_ENTRY = "directory_entry";
    private static final String KEYCHAIN = "keychain";

    private Database database;

    private static final String getAccountSQL = "SELECT * FROM " + ACCOUNT + " WHERE `user_name`=?;";
    private static final String addAccountSQL = "INSERT INTO " + ACCOUNT + " (`user_name`, `pwd_key`, `email`, `first_name`, `last_name`) VALUES (?,?,?,?,?);";
    private static final String updateAccountSQL = "UPDATE "+ACCOUNT + " SET `user_name`=?, `pwd_key`=?, `email`=?, `first_name`=?, `last_name`=? WHERE `user_name`=?;";
    private static final String removeAccountSQL = "DELETE FROM " + ACCOUNT + " WHERE `user_name`=?;";
    private static final String getDirectorySQL = "SELECT * FROM " + DIRECTORY + " WHERE `user_name`=?;";
    private static final String addDirectorySQL = "INSERT INTO " + DIRECTORY + " (`user_name`, `own_path`) VALUES (?,?);";
    private static final String removeDirectorySQL = "DELETE FROM " + DIRECTORY + " WHERE `user_name`=?;";
    private static final String addDirectoryEntrySQL = "INSERT INTO " + DIRECTORY_ENTRY + " (`directory_id`, `name`, `encryption_key`) VALUES (?,?,?);";
    private static final String updateDirectoryEntrySQL = "UPDATE " + DIRECTORY_ENTRY + " " + "SET `name`=?, `encryption_key`=? WHERE `directory_entry_id`=?;";
    private static final String getDirectoryEntrySQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `directory_entry_id`=?;";
    private static final String getDirectoryEntryWithNameSQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `directory_id`=? AND `name`=?;";
    private static final String getDirectoryEntriesSQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `directory_id`=?;";
    private static final String removeDirectoryEntrySQL = "DELETE FROM " + DIRECTORY_ENTRY + " WHERE `directory_entry_id`=?;";
    private static final String addKeychainSQL = "INSERT INTO " + KEYCHAIN + " (`directory_entry_id`, `password`, `metadata`) VALUES (?,?,?);";
    private static final String updateKeychainSQL = "UPDATE " + KEYCHAIN + " SET `password`=?, `metadata`=? WHERE `directory_entry_id`=?;";
    private static final String getKeychainSQL = "SELECT * FROM " + KEYCHAIN + " WHERE `directory_entry_id`=?;";
    private static final String removeKeychainSQL = "DELETE FROM " + KEYCHAIN + " WHERE `directory_entry_id`=?;";


    public DatabaseController(Database database) {
        this.database = database;
    }

    public Account getAccount(String username) {
        ResultSet rset = null;
        Account account = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            rset = statement.executeQuery();

            if (rset.next()) {
                account = new Account(username, rset.getString("pwd_key"), rset.getString("email"),
                        rset.getString("first_name"), rset.getString("last_name"));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }

        return account;
    }

    public boolean addAccount(Account account) {
        int add = 0;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addAccountSQL)) {

            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPassword()); // encrypt before storing
            statement.setString(3, account.getEmail());
            statement.setString(4, account.getFirstname());
            statement.setString(5, account.getLastname());

            add = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("An error occurred in addAccount"); // add duplication check here
        }

        return add == 1;
    }

    public boolean updateAccount(Account account) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateAccountSQL)) {

            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPassword()); // encrypt before storing
            statement.setString(3, account.getEmail());
            statement.setString(4, account.getFirstname());
            statement.setString(5, account.getLastname());
            statement.setString(6, account.getUsername());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Account does not exist");
        }

        return modify == 1;
    }

    public boolean removeAccount(String username) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeAccountSQL)) {
            statement.setString(1, username);
            remove = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Account did not exist");
        }

        return remove == 1;
    }

    public Directory getDirectory(String username) {
        ResultSet rset = null;
        Directory dir = null;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectorySQL)) {

            statement.setString(1, username);
            rset = statement.executeQuery();

            if (rset.next()) {
                dir = new Directory(rset.getInt("directory_id"), username, rset.getString("own_path"));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }

        return dir;
    }

    /**
     * @param username
     * @param ownPath
     * @return
     */
    public JSONObject addDirectory(String username, String ownPath) {
        int add = 0;
        int id = 0;
        boolean success = true;

        try(Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addDirectorySQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, ownPath);

            add = statement.executeUpdate();

            if (add == 1) {
                ResultSet rs = statement.getGeneratedKeys();
                rs.next();
                id = rs.getInt(1);
            }

            success = true;
        } catch (SQLException e) {
            System.out.println("An error occurred in addDirectory");
            success = false;
            e.printStackTrace();
        }

        JSONObject res = new JSONObject();
        if (success) {
            res.put("status", "success");
            res.put("directory_id", id);
        } else
            res.put("status", "failure");
        return res;
    }

    public boolean removeDirectory(String username) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeDirectorySQL)){
            statement.setString(1, username);
            remove = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Directory did not exist");
        }

        return remove == 1;
    }

    //TODO change this from JSONObject to DirectoryEntry
    public JSONObject addDirectoryEntry(String name, String encryptKey, int directoryId) {
        int add = 0;
        int id = 0;
        boolean success = false;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addDirectoryEntrySQL, Statement.RETURN_GENERATED_KEYS)){

            statement.setInt(1, directoryId);
            statement.setString(2, name);
            statement.setString(3, encryptKey);

            add = statement.executeUpdate();

            if (add == 1) {
                ResultSet rs = statement.getGeneratedKeys();
                rs.next();
                id = rs.getInt(1);
            }

            success = true;
        } catch (SQLException e) {
            System.out.println("An error occurred in addDirectoryEntry");
            success = false;
            e.printStackTrace();
        }

        JSONObject res = new JSONObject();
        if (success) {
            res.put("status", "success");
            res.put("directoryEntryId", id);
        } else
            res.put("status", "failure");

        return res;
    }

    public boolean updateDirectoryEntry(DirectoryEntry de) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateDirectoryEntrySQL)) {
            statement.setString(1, de.getName());
            statement.setString(2, de.getEncryptionKey());
            statement.setInt(3, de.getDirectoryEntryId());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DirectoryEntry does not exist");
        }

        return modify == 1;
    }

    public DirectoryEntry getDirectoryEntry(int directoryEntryId) {
        ResultSet rset = null;
        DirectoryEntry de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntrySQL)) {

            statement.setInt(1, directoryEntryId);
            rset = statement.executeQuery();

            if (rset.next()) {
                de = new DirectoryEntry(directoryEntryId, rset.getString("name"), rset.getString("encryption_key"), rset.getInt("directory_id"));
            }

        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return de;
    }

    public DirectoryEntry getDirectoryEntry(int directoryId, String name) {
        ResultSet rset = null;
        DirectoryEntry de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntryWithNameSQL)) {
            statement.setInt(1, directoryId);
            statement.setString(2, name);
            rset = statement.executeQuery();

            if (rset.next()) {
                de = new DirectoryEntry(rset.getInt("directory_entry_id"), name, rset.getString("encryption_key"), directoryId);
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return de;
    }

    public List<DirectoryEntry> getDirectoryEntries(int directoryId) {
        ResultSet rset = null;
        ArrayList<DirectoryEntry> list = new ArrayList<>();

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntriesSQL)) {
            statement.setInt(1, directoryId);
            rset = statement.executeQuery();

            while (rset.next()) {
                list.add(new DirectoryEntry(rset.getInt("directory_entry_id"), rset.getString("name"),
                        rset.getString("encryption_key"), rset.getInt("directory_id")));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return list;
    }

    public boolean removeDirectoryEntry(int directoryEntryId) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeDirectoryEntrySQL)){
            statement.setInt(1, directoryEntryId);
            remove = statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("DirectoryEntry did not exist");
        }

        return remove == 1;
    }

    public boolean addKeychain(int deId, String password, String metadata) {
        int add = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addKeychainSQL)){
            statement.setInt(1, deId);
            statement.setString(2, password);
            statement.setString(3, metadata);

            add = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("An error occurred in addDirectory");
        }
        return add == 1;
    }

    public boolean updateKeychain(Keychain key) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateKeychainSQL)){
            statement.setString(1, key.getPassword());
            statement.setString(2, key.getMetadata());
            statement.setInt(3, key.getDirectoryEntryId());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Keychain does not exist");
        }

        return modify == 1;
    }

    public Keychain getKeychain(int directoryEntryId) {
        ResultSet rset = null;
        Keychain de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getKeychainSQL)){
            statement.setInt(1, directoryEntryId);
            rset = statement.executeQuery();

            if (rset.next()) {
                de = new Keychain(directoryEntryId, rset.getString("password"), rset.getString("metadata"));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getKeychain");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return de;
    }

    public boolean removeKeychain(int directoryEntryId) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeKeychainSQL)){
            statement.setInt(1, directoryEntryId);
            remove = statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Keychain did not exist");
        }

        return remove == 1;
    }

}
