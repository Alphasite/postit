package postit.client.log;

import postit.client.controller.DirectoryController;
import postit.client.keychain.DirectoryEntry;
import postit.shared.AuditLog;
import postit.shared.AuditLog.EventType;
import postit.shared.AuditLog.LogEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KeychainLog {

	public static final String KEYCHAIN_LOG = AuditLog.LOG_DIR + "/keychain_log";
	
	private boolean initialized = true;
	
	public KeychainLog() {

	}

	public boolean isInitialized(){
		return initialized;
	}
	
	public void addCreateKeychainLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return; // do not care about failed creations
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_CREATE, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public void addUpdateKeychainLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return;
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_UPDATE, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public void addRemoveKeychainLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return;
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.KEYCHAIN_REMOVE, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public void addCreateShareLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return;
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_ADD, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public void addRemoveShareLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return;
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_REMOVE, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public void addUpdateShareLogEntry(DirectoryEntry directoryEntry, String username, boolean status, long keychainId, String message){
		if (! status) return;
		LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.SHARE_UPDATE, username, keychainId, status, message);
		directoryEntry.log.add(entry.toString());
	}
	
	public List<LogEntry> getKeychainLogEntries(DirectoryEntry directoryEntry){
		List<LogEntry> entries = new ArrayList<LogEntry>();

		for (String line : directoryEntry.getLog()) {
			try {
				LogEntry entry = AuditLog.parseLogEntry(line);
				entries.add(entry);
			} catch (Exception ignored) {
				// Dont care. ignore.
			}
		}

		entries.sort(Comparator.comparingLong(entry -> entry.time));

		return entries;
	}
	
	public void printLog(List<LogEntry> entries){
		for (LogEntry le: entries)
			System.out.println(le);
	}

	public void dumpLogs(DirectoryController controller) {
		// Creates log file if not existing
		File logDir = new File(AuditLog.LOG_DIR);
		if (!logDir.exists()) {
			boolean success = logDir.mkdirs();
			if (!success) {
				initialized = false;
				return;
			}
		}


		File log = new File(KEYCHAIN_LOG);
		if (!log.exists()) {
			try {
				if (!log.createNewFile()) {
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		List<LogEntry> allLogEntries = new ArrayList<>();
		List<DirectoryEntry> entries = controller.getKeychains();
		for (DirectoryEntry entry : entries) {
			List<LogEntry> keychainLogEntries = this.getKeychainLogEntries(entry);
			allLogEntries.addAll(keychainLogEntries);
		}

		allLogEntries.sort(Comparator.comparingLong(entry -> entry.time));

		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(KEYCHAIN_LOG), StandardCharsets.UTF_8), true)){
			for (LogEntry logEntry : allLogEntries) {
				writer.println(logEntry.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
