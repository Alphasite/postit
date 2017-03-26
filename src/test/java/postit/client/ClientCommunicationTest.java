package postit.client;

import postit.client.controller.RequestMessenger;
import postit.client.keychain.Account;
import postit.shared.communication.Client;

import javax.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class ClientCommunicationTest implements Runnable {
    Client client;

    public static void testRequest(String request, Client client) {
        System.out.println("Sending to server: " + request);
        Optional<JsonObject> response = client.send(request);
        assertThat(response.isPresent(), is(true));
        System.out.println("Response: " + response);
    }


    public ClientCommunicationTest(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        Account account = new Account("ning", "5431");
        String req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("ning", "5430");
        req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("mc", "5431");
        req = RequestMessenger.createAuthenticateMessage(account);
        testRequest(req, client);

        account = new Account("ning", "5430");
        req = RequestMessenger.createAddKeychainsMessage(account, "fb", "hihi", "1234", "");
        testRequest(req, client);
        req = RequestMessenger.createUpdateKeychainMessage(account, "fb", "lala", "4321", "nothing");
        testRequest(req, client);
        req = RequestMessenger.createUpdateKeychainMessage(account, "net", "lala", "4321", "nothing");
        testRequest(req, client);
        req = RequestMessenger.createGetKeychainsMessage(account);
        testRequest(req, client);
        req = RequestMessenger.createRemoveKeychainMessage(account, "net");
        testRequest(req, client);
        req = RequestMessenger.createRemoveKeychainMessage(account, "fb");
        testRequest(req, client);
        req = RequestMessenger.createGetKeychainsMessage(account);
        testRequest(req, client);
    }


    public static void main(String[] args) {
        Client client = new Client(2048, "localhost");
        ClientCommunicationTest test = new ClientCommunicationTest(client);
        Thread t3 = new Thread(test);
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(t3, 5, TimeUnit.SECONDS);
    }
}
