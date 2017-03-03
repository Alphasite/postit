package postit.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import postit.database.Database;
import postit.model.*;

public class DatabaseController {

	private static final String ACCOUNT = "account";
	
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
			stmt = conn.prepareStatement("UPDATE "+ACCOUNT+" SET "
					+ "(`user_name`, `pwd_key`, `email`, `first_name`, `last_name`)"
					+ "VALUES (?,?,?,?,?);");
			stmt.setString(1, account.getUsername());
			stmt.setString(2, account.getPassword()); // encrypt before storing
			stmt.setString(3, account.getEmail());
			stmt.setString(4, account.getFirstname()); 
			stmt.setString(5, account.getLastname());

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
	
	public void closeQuietly(PreparedStatement stmt){
		try {
			stmt.close();
		} catch (SQLException e) {
			// do nothing
		}
	}
	
	public void closeQuietly(Connection conn){
		try {
			conn.close();
		} catch (SQLException e) {
			// do nothing
		}
	}
}
