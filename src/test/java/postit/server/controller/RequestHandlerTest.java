package postit.server.controller;

import postit.client.controller.RequestMessenger;

import org.junit.Test;
import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class RequestHandlerTest {

	public static void testAuthentication(RequestHandler rh, String username, String pwd, boolean expected){
		System.out.printf("Authenticate %s with %s\n", username, pwd);
		String req = RequestMessenger.createAuthenticateMessage(username, pwd);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}
	
	public static void testAddKeychain(RequestHandler rh, String username, String name, String pwd, boolean expected){
		System.out.printf("Adding keychain to %s: (%s, %s)\n", username, name, pwd);
		String req = RequestMessenger.createAddKeychainsMessage(username, name, "haha", pwd, "nothing");
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}
	
	public static void testUpdateKeychain(RequestHandler rh, String username, String name, 
			String encryptKey, String password, String metadata, boolean expected){
		System.out.printf("Updating keychain to %s (%s,%s,%s,%s)\n", username, name, encryptKey, password, metadata);
		String req = RequestMessenger.createUpdateKeychainMessage(username, name, encryptKey, password, metadata);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}
	
	public static void testGetKeychains(RequestHandler rh, String username, int expectedNumKeychains){
		System.out.println("Getting keychains of " + username);
		String req = RequestMessenger.createGetKeychainsMessage(username);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success"));
		JSONArray ja = js.getJSONArray("keychains");
		assertEquals(expectedNumKeychains, ja.length());
	}
	
	public static void testRemoveKeychain(RequestHandler rh, String username, String name, boolean expected){
		System.out.printf("Removing keychain %s from %s\n", name, username);
		String req = RequestMessenger.createRemoveKeychainMessage(username, name);
		String res = rh.handleRequest(req);
		System.out.println(res);
		JSONObject js = new JSONObject(res);
		assertTrue(js.getString("status").equals("success") == expected);
	}
	
	@Test
	public void runTest() {
		RequestHandler rh = new RequestHandler();

		testAuthentication(rh, "ning", "5431", true);
		testAuthentication(rh, "ning", "5430", false);
		testAuthentication(rh, "mc", "5431", false);
		
		testAddKeychain(rh, "ning", "fb", "1234", true);
		testUpdateKeychain(rh, "ning", "fb", "lala", "4321", "nothing", true);
		testUpdateKeychain(rh, "ning", "net", "lala", "4321", "nothing", false);
		testGetKeychains(rh, "ning", 3);
		
		testRemoveKeychain(rh, "ning", "net", false);
		testRemoveKeychain(rh, "ning", "fb", true);
		testGetKeychains(rh, "ning", 2);
	}

}
