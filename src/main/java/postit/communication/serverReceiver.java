package postit.communication;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Created by dog on 3/14/2017.
 */
public class serverReceiver implements Runnable{
    ServerSocket serverSocket;
    Socket connection = null;
    DataInputStream in;
    int port;
    serverSender client;

    public serverReceiver(int port, serverSender client){
        this.port = port;
        this.client = client;
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

            //3. get in streams
            in = new DataInputStream(connection.getInputStream());
            //4. The two parts communicate via the input and output streams
            while (true){
            	if (in.available() > 0){
            		try {
            			Thread.sleep(2000);
            			JSONObject obj = readBuffer(in);

            			if (obj == null) continue;
            			else client.addRequest(obj);

            		} catch (Exception e) {
            			e.printStackTrace();
            		}
            	}
            }
        } catch(IOException ioException){
            ioException.printStackTrace();
        } finally {
            try{
                in.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    JSONObject readBuffer(DataInputStream in) throws Exception{
        //System.out.println("read buffer function");
        String line = in.readUTF();
        if (line.length() == 0) return null;
        JSONObject rtn = new JSONObject(line);
        System.out.println(rtn.toString());
        return rtn;
    }

}

