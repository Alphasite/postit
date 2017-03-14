package postit.client;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import postit.client.controller.RequestMessenger;
import postit.communication.Client;
import postit.communication.Server;

public class ClientCommunication {

    public static JsonObject stringToJsonObject(String msg){
    	JsonReader jsonReader = Json.createReader(new StringReader(msg));
    	JsonObject object = jsonReader.readObject();
    	jsonReader.close();
    	return object;
    }
    
    public static void testRequest(String request, Client sender, Server listener){   	
    	int reqId = sender.addRequest(request);
    	System.out.println("Sending to server: " + request);
    	String response = null;
    	while(true){ // block until request is received
    		try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		response = listener.getResponse(reqId);
			if (response != null)
				break;
    	}

    	JsonObject res = stringToJsonObject(response);
    	System.out.println("Response: " + response);
    }
    
    public static void main(String[] args) {

        // FOR CONNECTING TO THE POSTIT SERVER
        Client sender = new Client(2048, false);
        Server listener = new Server(4880, false, sender);

        Thread t1 = new Thread(listener);
        Thread t2 = new Thread(sender);

        t2.start();
        t1.start();
        
        String req = RequestMessenger.createAuthenticateMessage("ning", "5431");
        testRequest(req, sender, listener);
        req = RequestMessenger.createAuthenticateMessage("ning", "5430");
        testRequest(req, sender, listener);
//        req = RequestMessenger.createAuthenticateMessage("mc", "5431");
//        testRequest(req, sender, listener);
//
//        req = RequestMessenger.createAddKeychainsMessage("ning", "fb", null, "1234", "");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createUpdateKeychainMessage("ning", "fb", "lala", "4321", "nothing");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createUpdateKeychainMessage("ning", "net", "lala", "4321", "nothing");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createGetKeychainsMessage("ning");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createRemoveKeychainMessage("ning", "net");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createRemoveKeychainMessage("ning", "fb");
//        testRequest(req, sender, listener);
//        req = RequestMessenger.createGetKeychainsMessage("ning");
//        testRequest(req, sender, listener);
        
    	sender.stop();
    	listener.stop();
    }
}
