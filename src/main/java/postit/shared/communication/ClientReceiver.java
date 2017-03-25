package postit.shared.communication;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by dog on 3/14/2017.
 */
public class ClientReceiver implements Runnable {

    ServerSocket serverSocket;
    Socket connection = null;
    DataInputStream in;
    BufferedReader reader;
    int port;
    HashMap<Integer, String> table;

    public ClientReceiver(int port){
        this.port = port;
        table = new HashMap<>();
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

            //3. get Output streams
            in = new DataInputStream(connection.getInputStream());

            while (true){
            	if (in.available() > 0){
            		try {
            			JSONObject obj = readBuffer(in);
            			int id = (int) obj.get("id");
            			synchronized (table) {
            				table.put(id, (String) obj.get("response"));
            			}
            		} catch (Exception e){
            			e.printStackTrace();
            		}
            	}
            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //4: Closing connection
            try{
                in.close();
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
        synchronized (table) {
            if (table.get(requestId) == null) return null;
            return table.get(requestId);
        }
        // to create timeout message: MessagePackager.createTimeoutMessage();
    }

    JSONObject readBuffer(DataInputStream in) throws Exception{
        String line = in.readUTF();
        System.out.println("Got response: " + line);
        JSONObject rtn = new JSONObject(line);
        return rtn;
    }

}
