package postit.server.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import postit.shared.AuditLog;
import postit.shared.AuditLog.EventType;
import postit.shared.AuditLog.LogEntry;

public class AuthenticationLog {
	public AuthenticationLog(){
	}
	
	public void addAuthenticationLogEntry(String username, boolean status, String message){
	}
	
	public int getLatestNumFailedLogins(String username){

		return 0;
	}
}
