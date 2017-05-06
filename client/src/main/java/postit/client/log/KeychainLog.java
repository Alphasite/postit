package postit.client.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import postit.shared.AuditLog;
import postit.shared.AuditLog.EventType;
import postit.shared.AuditLog.LogEntry;

public class KeychainLog {

	public static final String KEYCHAIN_LOG = AuditLog.LOG_DIR + "/keychain_log";
	
	public KeychainLog(){
		// Creates log file if not existing
		File logDir = new File(AuditLog.LOG_DIR);
		if (! logDir.exists())
			logDir.mkdirs();
		
		File log = new File(KEYCHAIN_LOG);
		if (! log.exists())
			try {
				log.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		System.out.println(log.getAbsolutePath());
	}
	
	public void addCreateKeychainLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return; // do not care about failed creations
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_CREATE, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addUpdateKeychainLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_UPDATE, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addRemoveKeychainLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_REMOVE, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addCreateShareLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_ADD, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addRemoveShareLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_REMOVE, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public void addUpdateShareLogEntry(String username, boolean status, String keychainName, String message){
		if (! status) return;
		
		// Appends new log to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(KEYCHAIN_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_UPDATE, username, keychainName, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public List<LogEntry> getKeychainLogEntries(String keychainName){
		List<LogEntry> entries = new ArrayList<LogEntry>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(KEYCHAIN_LOG)));
			String line = reader.readLine();
			while (line != null){
				LogEntry entry = AuditLog.parseLogEntry(line);
				if (keychainName == null || entry.keychainName.equals(keychainName)){
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
		
		return entries;
	}
	
	public void printLog(List<LogEntry> entries){
		for (LogEntry le: entries)
			System.out.println(le);
	}
	
	public static void main(String[] args){
		KeychainLog kl = new KeychainLog();
		kl.addCreateKeychainLogEntry("ning", true, "keychain1", "added keychain keychain1");
		kl.addCreateKeychainLogEntry("ning", false, "keychain2", "added keychain2");
		kl.addCreateKeychainLogEntry("ning", true, "keychain3", "added keychain3");
		kl.addUpdateKeychainLogEntry("ning", true, "keychain1", "updated keychain1");
		kl.printLog(kl.getKeychainLogEntries("keychain1"));
	}
	
}
