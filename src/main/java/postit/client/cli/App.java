package postit.client.cli;

import postit.client.backend.BackingStore;
import postit.client.backend.BackingStoreImpl;
import postit.shared.Crypto;
import postit.client.backend.KeyService;
import postit.client.keychain.Directory;


import java.util.Optional;
import java.util.logging.Logger;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {
    private final static Logger LOGGER = Logger.getLogger(App.class.getName());

    private KeyService keyService;
    private BackingStore backingStore;

    public static void main(String[] args) {
        CLIKeyService keyService = new CLIKeyService();
        App app = new App(keyService, new BackingStoreImpl(keyService));


        if (app.init()) {
            app.run(args);
        } else {
            LOGGER.severe("Failed to initialise, exiting.");
        }
    }

    public App(KeyService keyService, BackingStore backingStore) {
        this.keyService = keyService;
        this.backingStore = backingStore;
    }

    // TODO remove.
    private boolean init() {
        if (!Crypto.init()) {
            return false;
        }

        if (!backingStore.init()) {
            return false;
        }

        return true;
    }

    private void run(String[] args) {
        Optional<Directory> directory = backingStore.readDirectory();

        if (!directory.isPresent()) {
            LOGGER.severe("Could not load directory.");
            return;
        }

        CommandLineParser.parse(args, keyService, directory.get());
    }
}
