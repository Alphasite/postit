package postit.communication;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import javax.json.*;


/**
 * Created by Zhan on 3/7/2017.
 */
public class Server implements Runnable {

    Vector<String> inQueue;
    ServerSocket serverSocket;
    Socket connection = null;
    OutputStreamWriter out;
    InputStreamReader in;
    BufferedReader reader;
    int port;

    public Server(Vector<String> queue, int port){
        this.inQueue = queue;
        this.port = port;
    }

    @Override
    public void run(){
        try{
            //1. creating a server socket
            serverSocket = new ServerSocket(port);
            //2. Wait for connection
            System.out.println("Waiting for connection");
            connection = serverSocket.accept();
            System.out.println("Connection received from " + connection.getInetAddress().getHostName());
            //3. get Input and Output streams
            out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            out.flush();
            in = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            //4. The two parts communicate via the input and output streams
            do{

                reader = new BufferedReader(in);
                JsonObject obj = readBuffer(reader);
                inQueue.add(obj.toString());

            }while(true);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //4: Closing connection
            try{
                in.close();
                out.close();
                serverSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Given a request id, retrieves the response that server sent.
     * Returns null if no response with that id has been received.
     * Should return time out message if no server side response after a threshold time.
     * @param requestId
     * @return
     */
    public String getResponse(int requestId){
    	
    	// to create timeout message: MessagePackager.createTimeoutMessage();
    	return null;
    }
    
    JsonObject readBuffer(BufferedReader reader){
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject obj = jsonReader.readObject();
        jsonReader.close();
        return obj;
    }
}
