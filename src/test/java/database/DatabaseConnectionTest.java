package database;

import java.sql.*;

/**
 * 
 * @author Ning
 *
 */
public class DatabaseConnectionTest {
	
	public static void main(String[] args) {
		try {
			Connection conn = Database.connectToDefault();

			Statement st = conn.createStatement();

			ResultSet rs = st.executeQuery("select * from account");

			if(rs.next()) {

				String name = rs.getString(1);

				String key = rs.getString(2);

				System.out.println(name+"\t"+key);
			}           

			rs.close();

			st.close();
			conn.close();
			
			System.out.println("test successful");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
