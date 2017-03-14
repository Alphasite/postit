package postit.communication;

import org.json.JSONObject;
import postit.server.controller.RequestHandler;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import javax.json.*;
/**
 * Created by Zhan on 3/7/2017.
 */
public class Client implements Runnable {

    Vector<JSONObject> outQueue;
    Socket clientSocket;
    OutputStreamWriter out;
    InputStreamReader in;
    int port;
    boolean postitServer;
    RequestHandler requestHandler;

    public Client(int port, boolean postitServer){
        this.outQueue = new Vector<>();
        this.port = port;
        this.postitServer = postitServer;
        if (postitServer){
            requestHandler = new RequestHandler();
        }
    }

    @Override
    public void run() {
        try {
            //1. creating a socket to connect to the server
            System.out.println("before connecting");
            boolean trying = true;
            while(trying){
                try{
                    clientSocket = new Socket("localhost", port);
                    trying = false;
                } catch (ConnectException e) {
                    System.out.println("Connect failed, waiting and trying again");
                    try {
                        Thread.sleep(2000);//2 seconds
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
            System.out.println("Connected to localhost in port " + port);
            //2. get Input and Output streams
            out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.flush();
            in = new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8);
            //3: Communicating with the server
            do {
                if (!outQueue.isEmpty()) {
                    if (postitServer){ // this is the server side client
                        JSONObject obj = outQueue.remove(0);
                        String response = requestHandler.handleRequest(obj.get("obj").toString());
                        int id = Integer.valueOf((Integer)obj.get("id"));
                        JSONObject toBeSent = new JSONObject();
                        toBeSent.put("id", id);
                        toBeSent.put("obj", toBeSent);
                        sendMessage(toBeSent);
                    }
                    else{
                        sendMessage(outQueue.remove(0));
                    }
                }
            } while (true);
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally{
            //4: Closing connection
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Adds the request to be sent to the server.
     * Returns the identification number of the request for retrieval.
     * @param req
     * @return
     */
    private static Random rnd = new Random();

    public static int getRandomNumber(int digCount) {
        StringBuilder sb = new StringBuilder(digCount);
        for(int i=0; i < digCount; i++)
            sb.append((char)('0' + rnd.nextInt(10)));
        return Integer.parseInt(sb.toString());
    }
    public int addRequest(String req){
        System.out.println("adding request");
    	JSONObject obj = new JSONObject(req);
    	JSONObject toBeSent = new JSONObject();
    	int id = getRandomNumber(8);
    	toBeSent.put("id", id);
    	toBeSent.put("obj", obj);
    	outQueue.add(obj);
    	return id;
    }

    public void addRequest(JSONObject obj){
        outQueue.add(obj);
    }

    void sendMessage(JSONObject obj) {
        try {
            out.write(obj.toString());
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
