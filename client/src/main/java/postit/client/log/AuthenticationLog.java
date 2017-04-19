package postit.client.log;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import postit.shared.AuditLog;
import postit.shared.AuditLog.*;

public class AuthenticationLog {
	public static final String AUTH_LOG = AuditLog.LOG_DIR + "/auth_log";
	
	public AuthenticationLog(){
		// Creates log file if not existing
		// Reads in log file for past failed attempts
		File logDir = new File(AuditLog.LOG_DIR);
		if (! logDir.exists())
			logDir.mkdirs();
		
		//File authLog = new File(AUTH_LOG);
		File log = new File(AUTH_LOG);
		if (! log.exists())
			try {
				log.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//System.out.println(new File(AUTH_LOG).getAbsolutePath());
	}
	
	public void addAuthenticationLogEntry(String username, boolean status, String message){
		// Appends new log in attempt to log
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(AUTH_LOG), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null){
			LogEntry entry = new LogEntry(System.currentTimeMillis(), EventType.AUTHENTICATE, username, status, message);
			writer.println(entry.toString());
			writer.close();
		}
	}
	
	public int getLatestNumFailedLogins(String username){
		// Returns the number of consecutive failed log-ins
		// GUI should lock up accordingly
		int numFailed = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(AUTH_LOG)));
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
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(AUTH_LOG)));
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
		return time;
	}
	
	public long getLastLoginTime(){
		return getLastLoginTime(null);
	}
	
	public static void main(String[] args){

//		AuthenticationLog log = new AuthenticationLog();
//		log.addAuthenticationLogEntry("ning", true, "hihi"); 
//		System.out.println(log.getLatestNumFailedLogins("ning"));
//		log.addAuthenticationLogEntry("ning", false, ":(");
//		System.out.println(log.getLatestNumFailedLogins("ning"));
		


		JOptionPane MsgBox = new JOptionPane("Login is diabled", JOptionPane.INFORMATION_MESSAGE);
		JDialog dlg = MsgBox.createDialog("Select Yes or No");
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dlg.addComponentListener(new ComponentAdapter() {
			@Override
			// =====================
			public void componentShown(ComponentEvent e) {
				// =====================
				super.componentShown(e);
				Timer t; t = new Timer(5000, new ActionListener() {
					@Override
					// =====================
					public void actionPerformed(ActionEvent e) {
						System.out.println("timer off");
					}
				});
				t.setRepeats(false);
				t.start();
			}
		});
		dlg.setVisible(true);
		Object n = MsgBox.getValue();
		System.out.println(n);
		System.out.println("Finished");
		dlg.dispose();
	} 
}
