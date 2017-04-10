package postit.client.communication;

import postit.client.backend.BackingStore;
import postit.shared.Crypto;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Zhan on 3/7/2017.
 */
public class Client {
    private final static Logger LOGGER = Logger.getLogger(Client.class.getName());

    private int port;
    private String url;

    public Client(int port, String url) {
        this.port = port;
        this.url = url;
    }

    public Optional<JsonObject> send(String request) {
        SSLSocketFactory factory = Crypto.getSSLContext().getSocketFactory();

        for (int attemptNumber = 0; attemptNumber < 3; attemptNumber++) {
            System.out.println("Sending message; Attempt Number " + attemptNumber);
            try (
                    final SSLSocket socket = (SSLSocket) factory.createSocket(url, port);
                    final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            ) {
                socket.setUseClientMode(true);
                socket.setSoTimeout(10000);
                socket.setEnabledCipherSuites(new String[]{"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"});

                System.out.println("Starting handshake...");
                socket.startHandshake();

                out.write(Base64.getEncoder().encodeToString(request.getBytes()));
                out.newLine();
                out.flush();

                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                String response = null;

                System.out.println("Blocking for next line...");
                while (!socket.isClosed()) {
                    char character = (char) in.read();

                    if (character == '\r' || character == '\n') {
                        response = responseStream.toString();
                        break;
                    }

                    responseStream.write(character);
                }

                if (response != null) {

                    JsonObject responseObject = Json.createReader(
                            new ByteArrayInputStream(Base64.getDecoder().decode(response))
                    ).readObject();

                    return Optional.of(responseObject);
                } else {
                    System.out.println("No response");
                }
            } catch (JsonException | IllegalStateException e) {
                LOGGER.warning("Error decrypting or parsing Json Response...: " + e.getMessage());
                return Optional.empty();
            } catch (SocketTimeoutException e) {
                LOGGER.warning("Socket timed out... retrying: " + e.getMessage());
            } catch (IOException e) {
                LOGGER.warning("IO error with socket: " + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
