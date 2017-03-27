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

@SuppressWarnings("FieldCanBeLocal")
public class DatabaseController {

    private static final String ACCOUNT = "account";
    private static final String DIRECTORY_ENTRY = "directory_entry";

    private Database database;

    private static final String getAccountSQL = "SELECT * FROM " + ACCOUNT + " WHERE `user_name`=?;";
    private static final String addAccountSQL = "INSERT INTO " + ACCOUNT + " (`user_name`, `pwd_key`, `email`, `first_name`, `last_name`, `salt`) VALUES (?,?,?,?,?,?);";
    private static final String updateAccountSQL = "UPDATE "+ACCOUNT + " SET `user_name`=?, `pwd_key`=?, `email`=?, `first_name`=?, `last_name`=? WHERE `user_name`=?;";
    private static final String removeAccountSQL = "DELETE FROM " + ACCOUNT + " WHERE `user_name`=?;";
    
    private static final String addDirectoryEntrySQL = "INSERT INTO " + DIRECTORY_ENTRY + " (`owner_user_name`, `name`, `data`) VALUES (?,?,?);";
    private static final String updateDirectoryEntrySQL = "UPDATE " + DIRECTORY_ENTRY + " " + "SET `name`=?, `data`=? WHERE `directory_entry_id`=?;";
    private static final String getDirectoryEntrySQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `directory_entry_id`=?;";
    private static final String getDirectoryEntryWithNameSQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `owner_user_name`=? AND `name`=?;";
    private static final String getDirectoryEntriesSQL = "SELECT * FROM " + DIRECTORY_ENTRY + " WHERE `owner_user_name`=?;";
    private static final String removeDirectoryEntrySQL = "DELETE FROM " + DIRECTORY_ENTRY + " WHERE `directory_entry_id`=?;";


    public DatabaseController(Database database) {
        this.database = database;
    }

    public ServerAccount getAccount(String username) {
        ResultSet resultSet;
        ServerAccount serverAccount = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                serverAccount = new ServerAccount(username, null, resultSet.getString("email"),
                        resultSet.getString("first_name"), resultSet.getString("last_name"));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }

        return serverAccount;
    }

    public String getSalt(String username){
        ResultSet resultSet;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
            	return resultSet.getString("salt");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }
        
        return null;
    }
    
    public String getPassword(String username){
        ResultSet resultSet;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getAccountSQL)) {
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
            	return resultSet.getString("pwd_key");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
        } catch (Exception e) {
            System.out.println("An error occurred"); // should be contained in JSONObject returned to view
        }
        
        return null;
    }
    
    public boolean addAccount(ServerAccount serverAccount) {
        int add = 0;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addAccountSQL)) {

            statement.setString(1, serverAccount.getUsername());
            statement.setString(2, serverAccount.getPassword()); // encrypt before storing
            statement.setString(3, serverAccount.getEmail());
            statement.setString(4, serverAccount.getFirstname());
            statement.setString(5, serverAccount.getLastname());
            statement.setString(6, serverAccount.getSalt());
            add = statement.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("An error occurred in addAccount: " + e.getMessage()); // add duplication check here
        }

        return add == 1;
    }

    public boolean updateAccount(ServerAccount serverAccount) {
        int modify = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(updateAccountSQL)) {

            statement.setString(1, serverAccount.getUsername());
            statement.setString(2, serverAccount.getPassword());
            statement.setString(3, serverAccount.getEmail());
            statement.setString(4, serverAccount.getFirstname());
            statement.setString(5, serverAccount.getLastname());
            statement.setString(6, serverAccount.getUsername());

            modify = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerAccount does not exist");
        }

        return modify == 1;
    }

    public boolean removeAccount(String username) {
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
    public JSONObject addDirectoryEntry(String ownerUsername, String name, String data) {
        int add;
        int id = 0;
        boolean success;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(addDirectoryEntrySQL, Statement.RETURN_GENERATED_KEYS)){

            statement.setString(1, ownerUsername);
            statement.setString(2, name);
            statement.setString(3, data);

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

    public boolean updateDirectoryEntry(ServerKeychain de) {
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

    public ServerKeychain getDirectoryEntry(long directoryEntryId) {
        ResultSet resultSet;
        ServerKeychain de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntrySQL)) {

            statement.setLong(1, directoryEntryId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                de = new ServerKeychain(
                        directoryEntryId, 
                        resultSet.getString("owner_user_name"),
                        resultSet.getString("name"), 
                        resultSet.getString("data")
                );
            }

        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return de;
    }

    public ServerKeychain getDirectoryEntry(String ownerUsername, String name) {
        ResultSet resultSet;
        ServerKeychain de = null;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntryWithNameSQL)) {
            statement.setString(1, ownerUsername);
            statement.setString(2, name);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                de = new ServerKeychain(
                        resultSet.getInt("directory_entry_id"), 
                        ownerUsername,
                        name,
                        resultSet.getString("data") 
                );
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return de;
    }

    public List<ServerKeychain> getDirectoryEntries(String ownerUsername) {
        ResultSet resultSet;
        ArrayList<ServerKeychain> list = new ArrayList<>();

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(getDirectoryEntriesSQL)) {
            statement.setNString(1, ownerUsername);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                list.add(new ServerKeychain(
                        resultSet.getInt("directory_entry_id"), 
                        resultSet.getString("owner_user_name"), 
                        resultSet.getString("name"),
                        resultSet.getString("data")
                ));
            }
        } catch (SQLException e) {
            System.out.println("An error occurred in getDirectoryEntry");
        } catch (Exception e) {
            System.out.println("An error occurred");
        }

        return list;
    }

    public boolean removeDirectoryEntry(long directoryEntryId) {
        int remove = 0;

        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(removeDirectoryEntrySQL)){
            statement.setLong(1, directoryEntryId);
            remove = statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServerKeychain did not exist");
        }

        return remove == 1;
    }
}
