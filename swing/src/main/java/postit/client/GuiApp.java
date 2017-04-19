package postit.client;

import postit.client.backend.BackingStore;
import postit.client.controller.ServerController;
import postit.client.gui.GUIKeyService;
import postit.client.gui.KeychainViewer;
import postit.client.log.AuthenticationLog;
import postit.shared.Crypto;
import postit.client.communication.Client;

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

        if (!Crypto.init()) {
            // TODO
        }

        if (!backingStore.init()) {
            // TODO
        }

        invokeLater(() -> {
            KeychainViewer kv = new KeychainViewer(serverController, backingStore, keyService);
        });
    }
}
