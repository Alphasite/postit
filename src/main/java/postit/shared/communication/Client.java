package postit.shared.communication;

import org.json.JSONObject;
import org.json.JSONWriter;
import postit.client.keychain.Account;
import postit.server.controller.RequestHandler;
import postit.server.database.Database;
import postit.shared.Crypto;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParsingException;
import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by Zhan on 3/7/2017.
 */
public class Client {

    private int port;
    private String url;

    public Client(int port, String url) {
        this.port = port;
        this.url = url;
    }

    public Optional<JsonObject> send(String request) {
        SSLSocketFactory factory = Crypto.getSSLContext().getSocketFactory();
        System.out.println(Arrays.toString(factory.getSupportedCipherSuites()));

        for (int attemptNumber = 0; attemptNumber < 3; attemptNumber++) {
            System.out.println("Sending message; Attempt Number " + attemptNumber);
            try (
                    SSLSocket socket = (SSLSocket) factory.createSocket(url, port);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    Scanner scanner = new Scanner(in)
            ) {
                socket.setUseClientMode(true);
                socket.setSoTimeout(10000);

                System.out.println("Starting handshake...");
                socket.startHandshake();
                System.out.println("Handshake done...");

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


                socket.close();

                if (response != null) {

                    JsonObject responseObject = Json.createReader(
                            new ByteArrayInputStream(Base64.getDecoder().decode(response))
                    ).readObject();

                    return Optional.of(responseObject);
                } else {
                    System.out.println("No response");
                }

//                if (scanner.hasNextLine()) {
//                    System.out.println("Received next line...");
//                    response = scanner.nextLine();
//
//                    socket.close();
//
//                    JsonObject responseObject = Json.createReader(
//                            new ByteArrayInputStream(Base64.getDecoder().decode(response))
//                    ).readObject();
//
//                    return Optional.of(responseObject);
//                } else {
//                    // TODO
//                    System.out.println("No response");
//                }

            } catch (JsonException | IllegalStateException e) {
                e.printStackTrace();
                return Optional.empty();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }
}
