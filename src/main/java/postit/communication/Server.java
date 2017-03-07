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
public class Server {

    Vector<JsonObject> inQueue;
    ServerSocket serverSocket;
    Socket connection = null;
    OutputStreamWriter out;
    InputStreamReader in;
    BufferedReader reader;
    int port;

    Server(Vector<JsonObject> queue, int port){
        this.inQueue = queue;
        this.port = port;
    }

    void run(){
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
                inQueue.add(obj);

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

    JsonObject readBuffer(BufferedReader reader){
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject obj = jsonReader.readObject();
        jsonReader.close();
        return obj;
    }
}
