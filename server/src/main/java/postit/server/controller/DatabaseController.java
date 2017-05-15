package postit.server.controller;

import org.json.JSONObject;
import postit.server.database.Database;
import postit.server.model.ServerAccount;
import postit.server.model.ServerKeychain;
import postit.shared.AuditLog;
import postit.shared.AuditLog.LogEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class DatabaseController {

    private static final String ACCOUNT = "account";
    private static final String DIRECTORY_ENTRY = "directory_entry";
    private static final String LOGIN = "login";

    private Database database;

    private static final String getAccountSQL
            = "SELECT * FROM " + ACCOUNT + " "
            + "WHERE `user_name`=?;";

    private static final String addAccountSQL
            = "INSERT INTO " + ACCOUNT + " (`user_name`, `pwd_key`, `email`, `first_name`, `last_name`, `phone_number`, `salt`, `key_pair`, `public_key`) VALUES (?,?,?,?,?,?,?,?,?);";

    private static final String updateAccountSQL
            = "UPDATE "+ACCOUNT + " SET `user_name`=?, `pwd_key`=?, `email`=?, `first_name`=?, `last_name`=?, `phone_number`=? "
            + "WHERE `user_name`=?;";

    private static final String updateAccountKeyPairSQL
            = "UPDATE "+ACCOUNT + " SET `keypair`=? "
            + "WHERE `user_name`=?;";

    private static final String removeAccountSQL
            = "DELETE FROM " + ACCOUNT + " "
            + "WHERE `user_name`=?;";
    
    private static final String addDirectoryEntrySQL
            = "INSERT INTO " + DIRECTORY_ENTRY + " "
            + "(`owner_user_name`, `owner_directory_entry_id`, `shared_user_name`, `shared_write_permission`, `name`, `data`) "
            + "VALUES (?,?,?,?,?,?);";

    private static final String updateDirectoryEntrySQL
            = "UPDATE " + DIRECTORY_ENTRY + " " + "SET `name`=?, `data`=? "
            + "WHERE `directory_entry_id`=?;";

    private static final String updateWriteableDirectoryEntrySQL
            = "UPDATE " + DIRECTORY_ENTRY + " " + "SET `shared_write_permission`=? "
            + "WHERE `owner_directory_entry_id`=? AND `shared_user_name`=?;";

    private static final String getDirectoryEntrySQL
            = "SELECT * FROM " + DIRECTORY_ENTRY + " "
            + "WHERE `directory_entry_id`=?;";

    private static final String getSharedInstancesOfDirectoryEntryForOwnerSQL
            = "SELECT * FROM " + DIRECTORY_ENTRY + " "
            + "WHERE `owner_user_name`=? AND `owner_directory_entry_id`=?;";

    private static final String getDirectoryEntriesSQL
            = "SELECT * FROM " + DIRECTORY_ENTRY + " "
            + "WHERE (`owner_user_name`=? AND `shared_user_name` IS NULL) OR `shared_user_name`=?;";

    private static final String removeDirectoryEntrySQL
            = "DELETE FROM " + DIRECTORY_ENTRY + " "
            + "WHERE `directory_entry_id`=?;";

    private static final String getLoginsSQL = "SELECT * FROM " + LOGIN + " WHERE `user_name`=? ORDER BY time;";
    private static final String addLoginSQL = "INSERT INTO " + LOGIN + " (`time`, `user_name`, `status`, `message`) VALUES (?,?,?,?);";
    
    public DatabaseController(Database database) {
        this.database = database;
    }

    ServerAccount getAccount(String username) {
        ResultSet resultSet;
        ServerAccount serverAccount = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                serverAccount = new ServerAccount(username, null, resultSet.getString("email"),
                        resultSet.getString("first_name"), resultSet.getString("last_name"), resultSet.getString("phone_number"));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount " + e.getMessage()); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage()); // should be contained in JSONObject returned to view
        }

        return serverAccount;
    }

    String getKeyPair(String username) {
        ResultSet resultSet;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("key_pair");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getKeyPair"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }

        return null;
    }

    String getPublickey(String username) {
        ResultSet resultSet;

        try (Connection connection = database.connect();
                PreparedStatement statement = connection.prepareStatement(getAccountSQL)){
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("public_key");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getPublickey");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return null;
    }

    String getSalt(String username){
        ResultSet resultSet;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
            	return resultSet.getString("salt");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getSalt"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }
        
        return null;
    }
    
    String getPassword(String username){
        ResultSet resultSet;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
            	return resultSet.getString("pwd_key");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getPassword"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }
        
        return null;
    }
    
    boolean addAccount(ServerAccount serverAccount) {
        int add = 0;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addAccountSQL)) {

            statement.setString(1, serverAccount.getUsername());
            statement.setString(2, serverAccount.getPassword()); // encrypt before storing
            statement.setString(3, serverAccount.getEmail());
            statement.setString(4, serverAccount.getFirstname());
            statement.setString(5, serverAccount.getLastname());
            statement.setString(6, serverAccount.getPhoneNumber());
            statement.setString(7, serverAccount.getSalt());
            statement.setString(8, serverAccount.getKeypair());
            statement.setString(9, serverAccount.getPublickey());
            add = statement.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("An error occurred in addAccount: " + e.getMessage()); // add duplication check here
        }

        return add == 1;
    }

    boolean updateAccount(ServerAccount serverAccount) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateAccountSQL)) {

            statement.setString(1, serverAccount.getUsername());
            statement.setString(2, serverAccount.getPassword());
            statement.setString(3, serverAccount.getEmail());
            statement.setString(4, serverAccount.getFirstname());
            statement.setString(5, serverAccount.getLastname());
            statement.setString(6, serverAccount.getPhoneNumber());
            statement.setString(7, serverAccount.getUsername());

            modify = statement.executeUpdate();

            if (serverAccount.getKeypair() != null) {
                modify = updateAccountKeyPair(serverAccount);
            }

        } catch (SQLException e) {
            System.out.println("ServerAccount does not exist");
        }

        return modify == 1;
    }

    int updateAccountKeyPair(ServerAccount serverAccount) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateAccountKeyPairSQL)) {

            statement.setString(1, serverAccount.getUsername());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerAccount does not exist");
        }

        return modify;
    }

    boolean removeAccount(String username) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeAccountSQL)) {
            statement.setString(1, username);
            remove = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerAccount did not exist");
        }

        return remove == 1;
    }

    //TODO change this from JSONObject to ServerKeychain
    JSONObject addDirectoryEntry(String ownerUsername, long ownerDirectoryId, String sharedUsername, boolean sharedCanWrite, String name, String data) {
        int add;
        int id = 0;
        boolean success;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addDirectoryEntrySQL, Statement.RETURN_GENERATED_KEYS)){

            statement.setString(1, ownerUsername);
            statement.setLong(2, ownerDirectoryId);
            statement.setString(3, sharedUsername);
            statement.setBoolean(4, sharedCanWrite);
            statement.setString(5, name);
            statement.setString(6, data);

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
        } else {
            res.put("status", "failure");
        }

        return res;
    }

    boolean updateDirectoryEntry(ServerKeychain de) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateDirectoryEntrySQL)) {
            statement.setString(1, de.getName());
            statement.setString(2, de.getData());
            statement.setLong(3, de.getDirectoryEntryId());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerKeychain does not exist");
        }

        return modify == 1;
    }

    ServerKeychain getDirectoryEntry(long directoryEntryId) {
        ResultSet resultSet;
        ServerKeychain de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntrySQL)) {

            statement.setLong(1, directoryEntryId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                de = resultSetToServerKeychain(resultSet);
            }

            return de;

        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
            return null;
        } catch (Exception e) {
            System.out.println("An error occurred");
            return null;
        }
    }

    List<ServerKeychain> getSharedInstancesOfDirectoryEntry(String ownerUsername, Long id) {
        ResultSet resultSet;
        ArrayList<ServerKeychain> list = new ArrayList<>();

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getSharedInstancesOfDirectoryEntryForOwnerSQL)) {
            statement.setString(1, ownerUsername);
            statement.setLong(2, id);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                list.add(resultSetToServerKeychain(resultSet));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAllInstancesOfDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return list;
    }

    List<ServerKeychain> getDirectoryEntries(String ownerUsername) {
        ResultSet resultSet;
        ArrayList<ServerKeychain> list = new ArrayList<>();

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntriesSQL)) {
            statement.setString(1, ownerUsername);
            statement.setString(2, ownerUsername);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                list.add(resultSetToServerKeychain(resultSet));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return list;
    }

    private static ServerKeychain resultSetToServerKeychain(ResultSet resultSet) throws SQLException {
        return new ServerKeychain(
                resultSet.getInt("directory_entry_id"),
                resultSet.getString("owner_user_name"),
                resultSet.getLong("owner_directory_entry_id"),
                resultSet.getString("shared_user_name"),
                resultSet.getBoolean("shared_write_permission"),
                resultSet.getString("name"),
                resultSet.getString("data")
        );
    }

    boolean removeDirectoryEntry(long directoryEntryId) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeDirectoryEntrySQL)){
            statement.setLong(1, directoryEntryId);
            remove = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerKeychain did not exist");
        }

        return remove == 1;
    }

    boolean setSharedKeychainWriteable(long id, String sharedUsername, boolean writeable) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateWriteableDirectoryEntrySQL)) {
            statement.setBoolean(1, writeable);
            statement.setLong(2, id);
            statement.setString(3, sharedUsername);

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SetSharedKeychainWriteable does not exist");
        }

        return modify == 1;
    }
    
    public List<LogEntry> getLogins(String username) {
        ResultSet resultSet;
        LogEntry log;

        List<LogEntry> list = new ArrayList<>();
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getLoginsSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
            	log = new LogEntry(resultSet.getTimestamp("time").getTime(), AuditLog.EventType.AUTHENTICATE, resultSet.getString("username"), 
            			-1, resultSet.getBoolean("status"), resultSet.getString("message"));
            	list.add(log);
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getLogins " + e.getMessage()); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage()); // should be contained in JSONObject returned to view
        }

        return list;
    }
    
    public boolean addLoginEntry(LogEntry log){
        int add = 0;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addLoginSQL)) {
        	statement.setTimestamp(1, new Timestamp(log.time));
            statement.setString(2, log.username);
            statement.setBoolean(3, log.status);
            statement.setString(4, log.message);
            add = statement.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("An error occurred in addLog: " + e.getMessage()); 
        }

        return add == 1;
    }
}
