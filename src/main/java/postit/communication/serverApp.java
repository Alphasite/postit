package postit.communication;

import javax.json.JsonObject;
import java.util.Vector;

/**
 * Created by dog on 3/8/2017.
 */
public class serverApp {
    public static void main(String[] args){
        int rePort = 2048;
        int outPort = 4880;

        Client processor = new Client(outPort, true);
        Server receiver = new Server(rePort, true, processor);
        receiver.run();
        processor.run();
    }
}