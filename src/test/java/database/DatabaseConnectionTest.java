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

			ResultSet rs1 = st.executeQuery("select * from account");

			if(rs1.next()) {

				String name = rs1.getString("user_name");

				String key = rs1.getString("pwd_key");
				
				System.out.printf("username: %s\tkey: %s\n", name, key);
				
				ResultSet rs2 = st.executeQuery("select * from directory where user_name='" + name + "'");
				
				String directoryid = "";
				if (rs2.next()){
					directoryid = rs2.getString("directory_id");
					System.out.println("\t" + "directory: " + directoryid);
				
					ResultSet rs3 = st.executeQuery("select * from directory_entry where directory_id='" + directoryid + "'");
					while (rs3.next()){
						String deid = rs3.getString("directory_entry_id");
						String dename = rs3.getString("name");
						String dekey = rs3.getString("encryption_key");
						
						Statement st2 = conn.createStatement();
						ResultSet rs4 = st2.executeQuery("select * from keychain where directory_entry_id ='" + deid + "'");
						String pwd = "";
						String data = "";
						if (rs4.next()){
							pwd = rs4.getString("password");
							data = rs4.getString("metadata");
						}
						
						rs4.close();
						System.out.printf("\t\tid: %s, name: %s, key: %s -- pwd: %s, metadata: %s\n", deid, dename, dekey, pwd, data);
					}
					
					rs3.close();
				}

				
				rs2.close();
			}           

			rs1.close();

			st.close();
			conn.close();
			
			System.out.println("test successful");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
