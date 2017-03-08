package postit.server.controller;

import org.json.JSONObject;

import postit.client.handler.RequestMessenger;

public class RequestHandlerTest {

	public static void testAuthentication(RequestHandler rh, String username, String pwd){
		System.out.printf("Authenticate %s with %s\n", username, pwd);
		String req = RequestMessenger.createAuthenticateMessage(username, pwd);
		System.out.println(rh.handleRequest(req));
	}
	
	public static void testAddKeychain(RequestHandler rh, String username, String name, String pwd){
		System.out.printf("Adding keychain to %s: (%s, %s)\n", username, name, pwd);
		String req = RequestMessenger.createAddKeychainsMessage(username, name, "haha", pwd, "nothing");
		System.out.println(rh.handleRequest(req));
	}
	
	public static void testUpdateKeychain(RequestHandler rh, String username, String name, 
			String encryptKey, String password, String metadata){
		System.out.printf("Updating keychain to %s (%s,%s,%s,%s)\n", username, name, encryptKey, password, metadata);
		String req = RequestMessenger.createUpdateKeychainMessage(username, name, encryptKey, password, metadata);
		System.out.println(rh.handleRequest(req));
	}
	
	public static void testGetKeychains(RequestHandler rh, String username){
		System.out.println("Getting keychains of " + username);
		String req = RequestMessenger.createGetKeychainsMessage(username);
		System.out.println(rh.handleRequest(req));
	}
	
	public static void testRemoveKeychain(RequestHandler rh, String username, String name){
		System.out.printf("Removing keychain %s from %s\n", name, username);
		String req = RequestMessenger.createRemoveKeychainMessage(username, name);
		System.out.println(rh.handleRequest(req));
	}
	
	public static void main(String[] args) {
		RequestHandler rh = new RequestHandler();

		testAuthentication(rh, "ning", "5431");
		testAuthentication(rh, "ning", "5430");
		testAuthentication(rh, "mc", "5431");
		
		testAddKeychain(rh, "ning", "fb", "1234");
		testUpdateKeychain(rh, "ning", "fb", "lala", "4321", "nothing");
		testUpdateKeychain(rh, "ning", "net", "lala", "4321", "nothing");
		testGetKeychains(rh, "ning");
		
		testRemoveKeychain(rh, "ning", "net");
		testRemoveKeychain(rh, "ning", "fb");
		testGetKeychains(rh, "ning");
	}

}
