package postit.client;

import postit.client.backend.BackingStore;
import postit.client.gui.GUIKeyService;
import postit.client.gui.KeychainViewer;
import postit.shared.Crypto;
import postit.shared.communication.ClientReceiver;
import postit.shared.communication.ClientSender;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * Created by nishadmathur on 23/3/17.
 */
public class GuiApp {
    public static void main(String[] args) {
        // FOR CONNECTING TO THE POSTIT SERVER
        ClientSender sender = new ClientSender(2048);
        ClientReceiver listener = new ClientReceiver(4880);

        Thread t1 = new Thread(listener);
        Thread t2 = new Thread(sender);

        t2.start();
        t1.start();

        invokeLater(() -> {
            GUIKeyService keyService = new GUIKeyService();
            BackingStore backingStore = new BackingStore(keyService);

            if (!Crypto.init()) {
                // TODO
            }
            if (!backingStore.init()) {
                // TODO
            }

            KeychainViewer kv = new KeychainViewer(backingStore, keyService);
        });
    }
}
