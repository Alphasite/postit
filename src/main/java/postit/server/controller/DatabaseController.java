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

public class DatabaseController {

	private static final String ACCOUNT = "account";
	private static final String DIRECTORY = "directory";
	private static final String DIRECTORY_ENTRY = "directory_entry";
	private static final String KEYCHAIN = "keychain";
	
	public Account getAccount(String username){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Account account = null;
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+ACCOUNT+" WHERE "
					+ "`user_name`=?;");

			stmt.setString(1, username);
			rset = stmt.executeQuery();
			
			if (rset.next()){
				account = new Account(username, rset.getString("pwd_key"), rset.getString("email"), 
						rset.getString("first_name"), rset.getString("last_name"));
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
		}
		catch(Exception e){
			System.out.println("An error occurred"); // should be contained in JSONObject returned to view
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return account;
	}
	
	public boolean addAccount(Account account){
		Connection conn = null;
		PreparedStatement stmt = null;
		int add = 0;
			conn = Database.connectToDefault();
			try {
				stmt = conn.prepareStatement("INSERT INTO "+ACCOUNT+" "
						+ "(`user_name`, `pwd_key`, `email`, `first_name`, `last_name`)"
						+ "VALUES (?,?,?,?,?);");
				stmt.setString(1, account.getUsername());
				stmt.setString(2, account.getPassword()); // encrypt before storing
				stmt.setString(3, account.getEmail());
				stmt.setString(4, account.getFirstname()); 
				stmt.setString(5, account.getLastname());
				
				add = stmt.executeUpdate();
			} 
			catch (SQLException e) {
				System.out.println("An error occurred in addAccount"); // add duplication check here
			}
			finally{
				closeQuietly(stmt);
				closeQuietly(conn);
			}
			
		return add == 1;
	}
	
	public boolean updateAccount(Account account){
		Connection conn = null;
		PreparedStatement stmt = null;
		int modify = 0;
		conn = Database.connectToDefault();
		try {
			stmt = conn.prepareStatement("UPDATE "+ACCOUNT+" SET `user_name`=?, `pwd_key`=?, `email`=?, "
					+ "`first_name`=?, `last_name`=? WHERE `user_name`=?;");
			stmt.setString(1, account.getUsername());
			stmt.setString(2, account.getPassword()); // encrypt before storing
			stmt.setString(3, account.getEmail());
			stmt.setString(4, account.getFirstname()); 
			stmt.setString(5, account.getLastname());
			stmt.setString(6, account.getUsername());
			
			modify = stmt.executeUpdate();
		} 
		catch (SQLException e) {
			System.out.println("Account does not exist");
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}

		return modify == 1;
	}
	
	public boolean removeAccount(String username){
		Connection conn = null;
		PreparedStatement stmt = null;
		int remove = 0;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("DELETE FROM "+ACCOUNT+" WHERE "
					+ "`user_name`=?;");
			stmt.setString(1, username);
			remove = stmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Account did not exist");
		} 
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return remove == 1;
	}
	
	public Directory getDirectory(String username){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Directory dir = null;
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+DIRECTORY+" WHERE "
					+ "`user_name`=?;");

			stmt.setString(1, username);
			rset = stmt.executeQuery();
			
			if (rset.next()){
				dir = new Directory(rset.getInt("directory_id"), username, rset.getString("own_path"));
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getAccount"); // should be contained in JSONObject returned to view
		}
		catch(Exception e){
			System.out.println("An error occurred"); // should be contained in JSONObject returned to view
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return dir;
	}
	
	/**
	 * 
	 * @param username
	 * @param ownPath
	 * @return
	 */
	public JSONObject addDirectory(String username, String ownPath){
		Connection conn = null;
		PreparedStatement stmt = null;
		int add = 0;
		int id = 0;
		boolean success = true;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("INSERT INTO "+DIRECTORY+" "
					+ "(`user_name`, `own_path`)"
					+ "VALUES (?,?);", 
					Statement.RETURN_GENERATED_KEYS);

			stmt.setString(1, username);
			stmt.setString(2, ownPath);
			
			add = stmt.executeUpdate();
			
			if (add == 1) {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				id = rs.getInt(1);
			}
			success = true;
		} 
		catch (SQLException e) {
			System.out.println("An error occurred in addDirectory");
			success = false;
			e.printStackTrace();
		} 
		finally {
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		JSONObject res = new JSONObject();
		if (success){
			res.put("status", "success");
			res.put("directory_id", id);
		}
		else
			res.put("status", "failure");
		return res;
	}
	
	public boolean removeDirectory(String username){
		Connection conn = null;
		PreparedStatement stmt = null;
		int remove = 0;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("DELETE FROM "+DIRECTORY+" WHERE "
					+ "`user_name`=?;");
			stmt.setString(1, username);
			remove = stmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Directory did not exist");
		} 
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return remove == 1;
	}
	
	public JSONObject addDirectoryEntry(String name, String encryptKey, int directoryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		int add = 0;
		int id = 0;
		boolean success = false;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("INSERT INTO "+DIRECTORY_ENTRY+" "
					+ "(`directory_id`, `name`, `encryption_key`)"
					+ "VALUES (?,?,?);", 
					Statement.RETURN_GENERATED_KEYS);

			stmt.setInt(1, directoryId);
			stmt.setString(2, name);
			stmt.setString(3, encryptKey); 
			
			add = stmt.executeUpdate();
			
			if (add == 1) {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				id = rs.getInt(1);
			}
			success = true;
		} 
		catch (SQLException e) {
			System.out.println("An error occurred in addDirectoryEntry");
			success = false;
			e.printStackTrace();
		} 
		finally {
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		JSONObject res = new JSONObject();
		if (success){
			res.put("status", "success");
			res.put("directoryEntryId", id);
		}
		else
			res.put("status", "failure");
		
		return res;
	}
	
	public boolean updateDirectoryEntry(DirectoryEntry de){
		Connection conn = null;
		PreparedStatement stmt = null;
		int modify = 0;
		conn = Database.connectToDefault();
		try {
			stmt = conn.prepareStatement("UPDATE "+DIRECTORY_ENTRY+" SET `name`=?, `encryption_key`=? WHERE `directory_entry_id`=?;");
			stmt.setString(1, de.getName());
			stmt.setString(2, de.getEncryptionKey()); 
			stmt.setInt(3, de.getDirectoryEntryId());
			
			modify = stmt.executeUpdate();
		} 
		catch (SQLException e) {
			System.out.println("DirectoryEntry does not exist");
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}

		return modify == 1;
	}
	
	public DirectoryEntry getDirectoryEntry(int directoryEntryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		DirectoryEntry de = null;
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+DIRECTORY_ENTRY+" WHERE "
					+ "`directory_entry_id`=?;");

			stmt.setInt(1, directoryEntryId);
			rset = stmt.executeQuery();
			
			if (rset.next()){
				de = new DirectoryEntry(directoryEntryId, rset.getString("name"), rset.getString("encryption_key"), rset.getInt("directory_id"));
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getDirectoryEntry"); 
		}
		catch(Exception e){
			System.out.println("An error occurred"); 
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return de;
	}
	
	public DirectoryEntry getDirectoryEntry(int directoryId, String name){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		DirectoryEntry de = null;
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+DIRECTORY_ENTRY+" WHERE "
					+ "`directory_id`=? AND `name`=?;");

			stmt.setInt(1, directoryId);
			stmt.setString(2, name);
			rset = stmt.executeQuery();
			
			if (rset.next()){
				de = new DirectoryEntry(rset.getInt("directory_entry_id"), name, rset.getString("encryption_key"), directoryId);
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getDirectoryEntry"); 
		}
		catch(Exception e){
			System.out.println("An error occurred"); 
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return de;
	}
	
	public List<DirectoryEntry> getDirectoryEntries(int directoryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		ArrayList<DirectoryEntry> list = new ArrayList<DirectoryEntry>();
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+DIRECTORY_ENTRY+" WHERE "
					+ "`directory_id`=?;");

			stmt.setInt(1, directoryId);
			rset = stmt.executeQuery();
			
			while (rset.next()){
				list.add(new DirectoryEntry(rset.getInt("directory_entry_id"), rset.getString("name"), 
						rset.getString("encryption_key"), rset.getInt("directory_id")));
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getDirectoryEntry"); 
		}
		catch(Exception e){
			System.out.println("An error occurred"); 
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return list;
	}
	
	public boolean removeDirectoryEntry(int directoryEntryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		int remove = 0;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("DELETE FROM "+DIRECTORY_ENTRY+" WHERE "
					+ "`directory_entry_id`=?;");
			stmt.setInt(1, directoryEntryId);
			remove = stmt.executeUpdate();
			
		} catch (SQLException e) {
			System.out.println("DirectoryEntry did not exist");
		} 
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return remove == 1;
	}
	
	public boolean addKeychain(int deId, String password, String metadata){
		Connection conn = null;
		PreparedStatement stmt = null;
		int add = 0;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("INSERT INTO "+KEYCHAIN+" "
					+ "(`directory_entry_id`, `password`, `metadata`)"
					+ "VALUES (?,?,?);");

			stmt.setInt(1, deId);
			stmt.setString(2, password);
			stmt.setString(3, metadata);
			
			add = stmt.executeUpdate();
		} 
		catch (SQLException e) {
			System.out.println("An error occurred in addDirectory");
		} 
		finally {
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return add == 1;
	}
	
	public boolean updateKeychain(Keychain key){
		Connection conn = null;
		PreparedStatement stmt = null;
		int modify = 0;
		conn = Database.connectToDefault();
		
		try {
			stmt = conn.prepareStatement("UPDATE "+KEYCHAIN+" SET `password`=?, `metadata`=? WHERE `directory_entry_id`=?;");
			stmt.setString(1, key.getPassword());
			stmt.setString(2, key.getMetadata()); 
			stmt.setInt(3, key.getDirectoryEntryId());
			
			modify = stmt.executeUpdate();
		} 
		catch (SQLException e) {
			System.out.println("Keychain does not exist");
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}

		return modify == 1;
	}
	
	public Keychain getKeychain(int directoryEntryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Keychain de = null;
		
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("SELECT * FROM "+KEYCHAIN+" WHERE "
					+ "`directory_entry_id`=?;");

			stmt.setInt(1, directoryEntryId);
			rset = stmt.executeQuery();
			
			if (rset.next()){
				de = new Keychain(directoryEntryId, rset.getString("password"), rset.getString("metadata"));
			}		
		}
		catch(SQLException e){
			System.out.println("An error occurred in getKeychain"); 
		}
		catch(Exception e){
			System.out.println("An error occurred"); 
		}
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		
		return de;
	}
	
	public boolean removeKeychain(int directoryEntryId){
		Connection conn = null;
		PreparedStatement stmt = null;
		int remove = 0;
		try {
			conn = Database.connectToDefault();
			stmt = conn.prepareStatement("DELETE FROM "+KEYCHAIN+" WHERE "
					+ "`directory_entry_id`=?;");
			stmt.setInt(1, directoryEntryId);
			remove = stmt.executeUpdate();
			
		} catch (SQLException e) {
			System.out.println("Keychain did not exist");
		} 
		finally{
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return remove == 1;
	}
	
	public void closeQuietly(PreparedStatement stmt){
		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException e) {
			// do nothing
		}
	}
	
	public void closeQuietly(Connection conn){
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			// do nothing
		}
	}
}
