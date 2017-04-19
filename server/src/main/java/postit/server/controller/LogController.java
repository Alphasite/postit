package postit.server.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import postit.shared.AuditLog;
import postit.shared.AuditLog.*;
import postit.shared.AuditLog.LogEntry;

public class LogController {

	private DatabaseController db;
	
	public LogController(DatabaseController db){
		this.db = db;
	}

	public void addAuthenticationLogEntry(String username, boolean status, String message){
		db.addLoginEntry(new LogEntry(System.currentTimeMillis(), EventType.AUTHENTICATE, username, status, message));
	}
	
	public int getLatestNumFailedLogins(String username){
		List<LogEntry> list = db.getLogins(username); // should be sorted by time
		int numFails = 0;
		for (LogEntry entry: list){
			if (entry.status)
				numFails = 0;
			else
				numFails++;
		}
		return numFails;
	}
	
	public long getLastLoginTime(String username){
		List<LogEntry> list = db.getLogins(username); // should be sorted by time
		return list.get(list.size()-1).time;
	}
}
