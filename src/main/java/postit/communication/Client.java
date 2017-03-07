package postit.communication;


import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import javax.json.*;

/**
 * Created by Zhan on 3/7/2017.
 */
public class Client {

    Vector<JsonObject> outQueue;
    Socket clientSocket;
    Socket connection = null;
    OutputStreamWriter out;
    InputStreamReader in;
    BufferedReader reader;
    int port;

    Client(Vector<JsonObject> queue, int port) {
        this.outQueue = queue;
        this.port = port;
    }

    void run() {
        try {
            //1. creating a socket to connect to the server
            clientSocket = new Socket("localhost", port);
            System.out.println("Connected to localhost in port 2004");
            //2. get Input and Output streams
            out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.flush();
            in = new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8);
            //3: Communicating with the server
            do {
                if (!outQueue.isEmpty()) {
                    sendMessage(outQueue.remove(0));
                }
            } while (true);
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
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

    void sendMessage(JsonObject obj) {
        try {
            out.write(obj.toString());
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
