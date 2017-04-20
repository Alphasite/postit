package postit.shared;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Keeps track of application events and log them in files
 * @author Ning
 *
 */
public class AuditLog {

	public static final String SYSTEM_DIR = "."; // the directory containing the system
	public static final String LOG_DIR = SYSTEM_DIR + "/logs";
	
	private static String FORMAT = "MMM dd yyyy kk:mm:ss";

	//TODO format to be decided
	/**
	 * Current format (update parseLogEntry() and LogEntry.toString() when this is changed:
	 * %{timestamp} %{event} %{username} %{status}: %{message}
	 */
	
	/**
	 *  Plan: AuditLog is a parent class with two children ClientLog and ServerLog and defines log formatting
	 *  - ClientLog keeps track of local events
	 *  	- Authentication log
	 *  		- Starts running when the app is started
	 *  		- Reads AUTH_LOG file to see how many failed attemps has occurred and see if log in should be limited
	 *  		- Adds to AUTH_LOG file whenever a log in is attempted and repeats previous step
	 *  		- Helps counter offline guessing attack but does not prevent (as someone can just break the file and not go thru GUI)
	 *  		- Only stores the log-ins of a period of time (a week?) unless log-in is under lock-down
	 *  	- Other events
	 *  		- Also encrypted using master key
	 *  		- Only runs when user has logged in
	 *  		- synced to server
	 *  - ServerLog keeps track of events for each Account and Keychain
	 *  	- Authentication log: same as client side but stored in DB
	 */
	
	public AuditLog(){
		
	}
	
	public static LogEntry parseLogEntry(String entry){
		DateFormat DATE_FORMAT = new SimpleDateFormat(FORMAT);
		String t = entry.substring(0, FORMAT.length());
		String rest = entry.substring(FORMAT.length() + 1);
		int sep = rest.indexOf(':');
		String[] parts = rest.substring(0, sep).split(" ");
		String msg = rest.substring(sep+2);
		
		if (parts.length < 3)
			throw new RuntimeException("Log entry's format is incorrect");
		
		long time;
		try {
			time = DATE_FORMAT.parse(t).getTime();
		} catch (ParseException e) {
			throw new RuntimeException("Log entry has incorrect time format");
		}
		return new LogEntry(time, EventType.valueOf(parts[0]), parts[1], parts[2].equals("success") ? true : false, msg);
	}
	
	public enum EventType{
		AUTHENTICATE
	}
	
	public static class LogEntry{
		public long time;
		public EventType event;
		public String username;
		public boolean status;
		public String message;
		
		public LogEntry(long time, EventType event, String username, boolean status, String message){
			this.time = time;
			this.event = event;
			this.username = username;
			this.status = status;
			this.message = message;
		}
		
		public String toString(){
			DateFormat DATE_FORMAT = new SimpleDateFormat(FORMAT);
			return String.format("%s %s %s %s: %s", DATE_FORMAT.format(new Date(System.currentTimeMillis())), event, username, status ? "success" : "failure", message);
		}
		
	}
}
