package postit.communication;

import org.json.JSONObject;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;


/**
 * Created by dog on 3/14/2017.
 */
public class ClientSender implements Runnable {
    Queue<JSONObject> outQueue;
    Socket clientSocket;
    DataOutputStream out;
    int port;

    public ClientSender(int port){
        this.outQueue = new ArrayDeque<>();
        this.port = port;
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
            out = new DataOutputStream(clientSocket.getOutputStream());
            //3: Communicating with the server
            while(true) {
                synchronized (outQueue) {
                    if (!outQueue.isEmpty()) {
                        sendMessage(outQueue.remove());
                    }
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
        //System.out.println("adding request");
        JSONObject toBeSent = new JSONObject();
        int id = getRandomNumber(8);
        toBeSent.put("id", id);
        toBeSent.put("req", req);

        synchronized (outQueue) {
            outQueue.add(toBeSent);
        }

        return id;
    }

    void sendMessage(JSONObject obj) {
        try {
            out.writeUTF(obj.toString());
            //System.out.println("sent");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
