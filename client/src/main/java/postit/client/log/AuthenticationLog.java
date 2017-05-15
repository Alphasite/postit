package postit.client.log;

import postit.shared.AuditLog;
import postit.shared.AuditLog.EventType;
import postit.shared.AuditLog.LogEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AuthenticationLog {
	public static final String AUTH_LOG = AuditLog.LOG_DIR + "/auth_log";
	
	private boolean initialized = true;
	
	public AuthenticationLog(){
		// Creates log file if not existing
		// Reads in log file for past failed attempts
		File logDir = new File(AuditLog.LOG_DIR);
		if (! logDir.exists()){
			boolean success = logDir.mkdirs();
			if(!success){
				initialized = false;
				return;
			}
		}


		
		//File authLog = new File(AUTH_LOG);
		File log = new File(AUTH_LOG);
		if (! log.exists())
			try {
				boolean success = log.createNewFile();
				if(!success){
					initialized = false;
					return;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//System.out.println(new File(AUTH_LOG).getAbsolutePath());
	}
	
	public boolean isInitialized(){
		return initialized;
	}
	
	public void addAuthenticationLogEntry(String username, boolean status, String message){
		// Appends new log in attempt to log
		PrintWriter writer = null;
		try {

			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(AUTH_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.AUTHENTICATE, username, -1, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public int getLatestNumFailedLogins(String username){
		// Returns the number of consecutive failed log-ins
		// GUI should lock up accordingly
		int numFailed = 0;
		BufferedReader reader =null;
		try {

			reader = new BufferedReader(new InputStreamReader(new FileInputStream(AUTH_LOG), StandardCharsets.UTF_8));
			String line = reader.readLine();
			while (line != null){
				LogEntry entry = AuditLog.parseLogEntry(line);
				if (username == null || entry.username.equals(username)){
					if (entry.status)
						numFailed = 0;
					else
						numFailed++;
				}
				line = reader.readLine();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (reader != null) reader.close();;
			} catch (IOException io) {
				//log exception here
			}
		}
		return numFailed;
	}


	public int getLatestNumFailedLogins(){
		return getLatestNumFailedLogins(null);
	}
	
	public long getLastLoginTime(String username){
		// Returns the number of consecutive failed log-ins
		// GUI should lock up accordingly
		long time = -1; // -1 if never logged in before
		BufferedReader reader = null;
		try {
			 reader = new BufferedReader(new InputStreamReader(new FileInputStream(AUTH_LOG), StandardCharsets.UTF_8));
			String line = reader.readLine();
			while (line != null){
				LogEntry entry = AuditLog.parseLogEntry(line);
				if (username == null || entry.username.equals(username)){
					time = entry.time;
				}
				line = reader.readLine();
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (reader != null) reader.close();;
			} catch (IOException io) {
				//log exception here
			}
		}
		return time;
	}
	
	public long getLastLoginTime(){
		return getLastLoginTime(null);
	}

}
