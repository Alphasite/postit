package postit.client;

import java.io.StringReader;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import postit.client.controller.RequestMessenger;
import postit.communication.*;


public class ClientCommunication implements Runnable{
	clientSender s;
	clientReceiver r;

	public static JsonObject stringToJsonObject(String msg){
		JsonReader jsonReader = Json.createReader(new StringReader(msg));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		return object;
	}

	public static void testRequest(String request, clientSender sender, clientReceiver listener){
		int reqId = sender.addRequest(request);
		System.out.println("Sending to server: " + request);
		String response = null;
		while(true){ // block until request is received
			try {
				Thread.sleep(2000);
				System.out.println("sleeping.....");
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

	public ClientCommunication(clientSender s, clientReceiver r){
		this.s = s;
		this.r = r;
	}

	@Override
	public void run(){
		String req = RequestMessenger.createAuthenticateMessage("ning", "5431");
		testRequest(req, s, r);
		req = RequestMessenger.createAuthenticateMessage("ning", "5430");
		testRequest(req, s, r);
		req = RequestMessenger.createAuthenticateMessage("mc", "5431");
		testRequest(req, s, r);
		req = RequestMessenger.createAddKeychainsMessage("ning", "fb", null, "1234", "");
		testRequest(req, s, r);
		req = RequestMessenger.createUpdateKeychainMessage("ning", "fb", "lala", "4321", "nothing");
		testRequest(req, s, r);
		req = RequestMessenger.createUpdateKeychainMessage("ning", "net", "lala", "4321", "nothing");
		testRequest(req, s, r);
		req = RequestMessenger.createGetKeychainsMessage("ning");
		testRequest(req, s, r);
		req = RequestMessenger.createRemoveKeychainMessage("ning", "net");
		testRequest(req, s, r);
		req = RequestMessenger.createRemoveKeychainMessage("ning", "fb");
		testRequest(req, s, r);
		req = RequestMessenger.createGetKeychainsMessage("ning");
		testRequest(req, s, r);
	}


	public static void main(String[] args) {

		// FOR CONNECTING TO THE POSTIT SERVER
		clientSender sender = new clientSender(2048);
		clientReceiver listener = new clientReceiver(4880);

		Thread t1 = new Thread(listener);
		Thread t2 = new Thread(sender);

		t2.start();
		t1.start();

		ClientCommunication client = new ClientCommunication(sender, listener);
		Thread t3 = new Thread(client);
		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		executor.schedule(t3, 5, TimeUnit.SECONDS);

	}
}
