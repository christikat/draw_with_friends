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

    String username = null; // clients username

    /**
     * Constructor for ClientHandler.
     * 
     * @param socket the client socket
     * @param server reference to the server
     */
    public ClientHandler(Socket socket, DrawingServer server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * Sends a message from server to client.
     * 
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

            // must receive username before anything else
            while (username == null) {
                String line = input.readLine();
                if (line == null) {
                    server.log("Client disconnected before sending username");
                    return;
                }

                // handles "JOIN" message, sent when client GUI is loaded
                if (line.startsWith("JOIN ")) {
                    String proposedUsername = line.substring(5).trim();
                    if (proposedUsername.isEmpty() || server.isUsernameTaken(proposedUsername)) {
                        sendMessage("NAMEINUSE");
                        continue; // bcuz we want to keep asking for a username
                    } else {
                        this.username = proposedUsername;
                        server.addUsername(this.username);

                        sendMessage("JOINED " + this.username);
                        server.broadcastUserList();

                        server.log("User joined: " + this.username);
                        break; // exit the loop
                    }
                }
            }

            // after the loop, username is set
            while (!isReady) {
                String line = input.readLine();
                if (line == null) {
                    // user disconnected
                    server.removeClient(this);
                    return;
                }
                if (line.equals("READY")) {
                    isReady = true;
                    // sync canvas
                    for (String d : server.getHistory()) {
                        sendMessage(d);
                    }
                    server.broadcastUserList();
                    // if they're the only user => they get the turn
                    if (server.getNumClients() == 1) {
                        server.setClientTurn();
                    }
                    break;
                }
            }

            // main loop
            
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }

                // Handles "READY" message, sent when client GUI is loaded
                if (!isReady) {
                    if (line.equals("READY")) {
                        isReady = true;
                        // sync canvas with current drawing
                        for (String drawData : server.getHistory()) {
                            output.println(drawData);
                        }

                        // If they are the only client - it's their turn
                        if (server.getNumClients() == 1) {
                            server.setClientTurn();
                        }
                    }
                    continue;
                }

                if (line.equals("ENDTURN")) {
                    // Check if it's their turn
                    if (server.getCurrentClientTurn() == this) {
                        server.log("Player" + username + " ended their turn.");
                        server.updateTurn();
                    }
                } else if (line.startsWith("DRAW")) {
                    // Send the new drawing data to other clients, and store data in history
                    if (server.getCurrentClientTurn() == this) {
                        server.sendDrawData(this, line);
                        server.updateHistory(line);
                    } else {
                        sendMessage("Not your turn!");
                    }
                } else if (line.startsWith("LOADIMG ")) {
                    // restricted to current drawer
                    if (server.getCurrentClientTurn() == this) {
                        server.broadcastMessage(line);
                        server.log("User" + username + " loaded image: " + line.substring(8));
                    } else {
                        sendMessage("Not your turn!");
                    }
                } else {
                    server.log("Unknown message from " + username + ": " + line);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeEverything();

            server.removeClient(this);
            server.log("Client disconnected: " + (username != null ? username : socket));
        }
    }

    private void closeEverything() {
        try {
            if (input != null)
                input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (output != null)
            output.close();

        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
