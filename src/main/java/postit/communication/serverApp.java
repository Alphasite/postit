package postit.communication;

import javax.json.JsonObject;
import java.util.Vector;

/**
 * Created by dog on 3/8/2017.
 */
public class serverApp {
    public static void main(String[] args){
        Vector<String> inQueue = new Vector<>();
        Vector<String> outQueue = new Vector<>();
        int rePort = 2048;
        int outPort = 4880;

        Thread receiver = new Server(inQueue, rePort);
        Thread processor = new Client(outQueue, outPort, false);
        receiver.start();
        processor.start();
    }
}