package us.otechu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles server communication with a client.
 * Each instance runs on its own thread.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DrawingServer server;
    BufferedReader input = null;
    PrintWriter output = null;

    /** True when client GUI is ready for server messages */
    private boolean isReady = false;

    // TODO: Get username from client
    String username;

    /**
     * Constructor for ClientHandler.
     * @param socket the client socket
     * @param server reference to the server
     */
    public ClientHandler(Socket socket, DrawingServer server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * Sends a message from server to client.
     * @param message the message to send
     */
    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        }
    }
    /**
     * Loop for handling messages from the client.
     * Processes READY, DRAW, and ENDTURN messages.
     */

    public boolean getIsReady() {
        return isReady;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = input.readLine()) != null) {
                // Handles "READY" message, sent when client GUI is loaded
                if (!isReady) {
                    if (line.equals("READY")) {
                        isReady = true;
                        //sync canvas with current drawing
                        for (String drawData: server.getHistory()) {
                            output.println(drawData);
                        }

                        //If they are the only client - it's their turn
                        if (server.getNumClients() == 1) {
                            server.setClientTurn();
                        }
                    }
                }

                if (line.equals("ENDTURN")) {
                    // Check if it's their turn
                    if (server.getCurrentClientTurn() == this) {
                        server.log("Player ended turn");
                        server.updateTurn();
                    }
                } else if (line.startsWith("DRAW")) {
                    // Send the new drawing data to other clients, and store data in history
                    server.sendDrawData(this, line);
                    server.updateHistory(line);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            try {
                if (input != null) { input.close(); }
                if (output != null) { output.close(); }
                socket.close();

                server.removeClient(this);
                server.log("Client disconnect");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
