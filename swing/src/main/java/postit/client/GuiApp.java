package postit.client;

import postit.client.backend.BackingStore;
import postit.client.communication.Client;
import postit.client.controller.ServerController;
import postit.client.gui.GUIKeyService;
import postit.client.gui.KeychainViewer;
import postit.client.log.AuthenticationLog;
import postit.client.log.KeychainLog;
import postit.shared.Crypto;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * Created by nishadmathur on 23/3/17.
 */
public class GuiApp {
    public static void main(String[] args) {
        Client client = new Client(2048, "localhost");
        ServerController serverController = new ServerController(client);
        AuthenticationLog authLog = new AuthenticationLog();

        GUIKeyService keyService = new GUIKeyService(serverController, authLog);
        BackingStore backingStore = new BackingStore(keyService);

        keyService.setBackingStore(backingStore);
        serverController.setKeyService(keyService);

        if (! authLog.isInitialized()){
            System.exit(0);
        }
        
        if (!Crypto.init()) {
            System.out.println("Crypto could not be initialized. ABORTING");
            System.exit(0);
        }

        if (!backingStore.init()) {
            System.out.println("backingStore could not be initialized. ABORTING");
            System.exit(0);
        }

        invokeLater(() -> {
        	KeychainLog keyLog = new KeychainLog();
            KeychainViewer kv = new KeychainViewer(serverController, backingStore, keyService, keyLog, authLog);
            if (! keyLog.isInitialized()){
                System.exit(0);
            }
        });
    }
}
