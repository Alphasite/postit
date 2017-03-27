package postit.server.database;

import java.sql.*;

import org.junit.Before;
import org.junit.Test;
import postit.server.controller.DatabaseController;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * 
 * @author Ning
 *
 */
public class DatabaseConnectionTest {
	Database database;
	DatabaseController db;

	@Before
	public void setUp() throws Exception {
		database = new TestH2();
		db = new DatabaseController(database);

		assertThat(database.initDatabase(), is(true));
	}

	@Test
	public void runTest() throws Exception {
		try (Connection conn = database.connect(); Statement st = conn.createStatement()) {
			assertNotNull(conn);

			ResultSet rs1 = st.executeQuery("SELECT * FROM account");

			if (rs1.next()) {

				String name = rs1.getString("user_name");

				String key = rs1.getString("pwd_key");

				ResultSet rs3 = st.executeQuery("select * from directory_entry where owner_user_name='" + name + "'");
				while (rs3.next()) {
					String deid = rs3.getString("directory_entry_id");
					String dename = rs3.getString("name");
					String dedata = rs3.getString("data");
					System.out.printf("\t\tid: %s, name: %s, data: %s%n", deid, dename, dedata);
				}

				rs3.close();
			}

			rs1.close();

			System.out.println("test successful");
		}
	}

}
