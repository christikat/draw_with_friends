package us.otechu.client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
/**
 * Handles sending messages to the server and receiving messages from the server
 */
public class ClientConnection {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    /** Thread that listens for incoming server messages */
    private Thread serverListener;

    /** Function that handles server messages */
    private Consumer<String> messageHandler; // handles incoming messages



    /**
     * Creates connection to the server and listens for messages.
     * @param messageHandler A function to handle messages from the server
     * @throws IOException if the connection fails
     */
    public ClientConnection(Consumer<String> messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.socket = new Socket(SERVER_ADDRESS, PORT);

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        startListener();
    }
    /**
     * Thread that continuously listens for server messages and sends them to the message handler.
     */
    private void startListener() {
        serverListener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    messageHandler.accept(line);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            } finally {
                if (socket != null && !socket.isClosed()) {
                    disconnect();
                }
            }
        });
        serverListener.start();
    }


    /**
     * Sends a message to the server.
     * @param message The message to send
     */
    public void send(String message) {
        out.println(message);
    }

    /**
     * Closes socket and ends connection to the server.
     */
    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}