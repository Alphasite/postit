package postit.client.log;

import postit.shared.AuditLog;
import postit.shared.AuditLog.EventType;
import postit.shared.AuditLog.LogEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KeychainLog {

	public static final String KEYCHAIN_LOG = AuditLog.LOG_DIR + "/keychain_log";
	
	private boolean initialized = true;
	
	public KeychainLog(){
		// Creates log file if not existing
		File logDir = new File(AuditLog.LOG_DIR);
		if (! logDir.exists()){
			boolean success = logDir.mkdirs();
			if(!success){
				initialized = false;
				return;
			}
		}

		
		File log = new File(KEYCHAIN_LOG);
		if (! log.exists())
			try {
				boolean success = log.createNewFile();
				if(!success){
					initialized = false;
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

	}
	
	public boolean isInitialized(){
		return initialized;
	}
	
	public void addCreateKeychainLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return; // do not care about failed creations
		
		// Appends new log to file
		PrintWriter writer = null;
		try {

			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_CREATE, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addUpdateKeychainLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_UPDATE, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addRemoveKeychainLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_REMOVE, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addCreateShareLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_ADD, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addRemoveShareLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_REMOVE, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addUpdateShareLogEntry(String username, boolean status, long keychainId, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_UPDATE, username, keychainId, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public List<LogEntry> getKeychainLogEntries(long keychainId){
		List<LogEntry> entries = new ArrayList<LogEntry>();
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new InputStreamReader(new FileInputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8));
			String line = reader.readLine();
			while (line != null){
				LogEntry entry = AuditLog.parseLogEntry(line);
				if (keychainId == -1 || entry.keychainId == keychainId){
					entries.add(entry);
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
		
		return entries;
	}
	
	public void printLog(List<LogEntry> entries){
		for (LogEntry le: entries)
			System.out.println(le);
	}
	
	public static void main(String[] args){
		KeychainLog kl = new KeychainLog();
		kl.addCreateKeychainLogEntry("ning", true, 1, "added keychain keychain1");
		kl.addCreateKeychainLogEntry("ning", false, 2, "added keychain2");
		kl.addCreateKeychainLogEntry("ning", true, 3, "added keychain3");
		kl.addUpdateKeychainLogEntry("ning", true, 1, "updated keychain1");
		kl.printLog(kl.getKeychainLogEntries(1));
	}
	
}
