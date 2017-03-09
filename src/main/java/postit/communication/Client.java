package postit.communication;

import postit.server.controller.RequestHandler;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import javax.json.*;
/**
 * Created by Zhan on 3/7/2017.
 */
public class Client implements Runnable {

    Vector<String> outQueue;
    Socket clientSocket;
    OutputStreamWriter out;
    InputStreamReader in;
    int port;
    boolean postitServer;
    RequestHandler requestHandler;

    public Client(Vector<String> queue, int port, boolean ifClientSide){
        this.outQueue = queue;
        this.port = port;
        this.postitServer = ifClientSide;
        if (!postitServer){
            requestHandler = new RequestHandler();
        }
    }

    @Override
    public void run() {
        try {
            wait(5000);
            //1. creating a socket to connect to the server
            clientSocket = new Socket("localhost", port);
            System.out.println("Connected to localhost in port " + port);
            //2. get Input and Output streams
            out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.flush();
            in = new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8);
            //3: Communicating with the server
            do {
                if (!outQueue.isEmpty()) {
                    sendMessage(outQueue.remove(0));
                    if (!postitServer){ // this is the server side client
                        requestHandler.handleRequest(outQueue.remove(0).toString());
                    }
                }
            } while (true);
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException to){
            to.printStackTrace();
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
    public int addRequest(String req){
    	
    	return -1;
    }
    
    void sendMessage(String obj) {
        try {
            out.write(obj);
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
