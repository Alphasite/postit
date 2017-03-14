package postit.communication;

import javax.json.JsonObject;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dog on 3/8/2017.
 */
public class serverApp {
    public static void main(String[] args){
        int rePort = 2048;
        int outPort = 4880;

        Client processor = new Client(4880, true);
        Server receiver = new Server(2048, true, processor);
        Thread t1 = new Thread(processor);
        Thread t2 = new Thread(receiver);

//        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
//
//        executor.schedule(t2, 5, TimeUnit.SECONDS);
//        executor.schedule(t1, t0, TimeUnit.SECONDS);
        t1.start();
        t2.start();

    }
}