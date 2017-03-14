package postit.communication;

import org.json.JSONObject;
import postit.server.controller.RequestHandler;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Zhan on 3/7/2017.
 */
public class serverSender implements Runnable {

    Queue<JSONObject> outQueue;
    Socket clientSocket;
    DataOutputStream out;
    int port;
    RequestHandler requestHandler;

    public serverSender(int port){
        this.outQueue = new ArrayDeque<>();
        this.port = port;
        requestHandler = new RequestHandler();
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
            out = new DataOutputStream(clientSocket.getOutputStream());
            //3: Communicating with the server
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e){

                }

                //System.out.println("server sender loop");
                boolean ifEmpty;

                synchronized (outQueue) {
                    ifEmpty = outQueue.isEmpty();
                }

                if (!ifEmpty) {

                    JSONObject obj = null;

                    synchronized (outQueue) {
                        obj = outQueue.remove();
                    }

                    String response = requestHandler.handleRequest((String)obj.get("req"));
                    int id = Integer.valueOf((int)obj.get("id"));
                    JSONObject toBeSent = new JSONObject();
                    toBeSent.put("id", id);
                    toBeSent.put("response", response);
                    sendMessage(toBeSent);
                }
            }
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally{
            //4: Closing connection
            try {
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
     * @param obj
     * @return
     */

    public void addRequest(JSONObject obj){
        synchronized (outQueue) {
            //System.out.println("server sender adding requests");
            outQueue.add(obj);
            //System.out.println(outQueue.size());
        }
    }

    void sendMessage(JSONObject obj) {
        try {
            out.writeUTF(obj.toString());
            //System.out.println("Response sent");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
