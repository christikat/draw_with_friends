package us.otechu.server;

import java.awt.image.BufferedImage;
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
    private BufferedReader input;
    private PrintWriter output;

    // true only after the client has fully loaded + sent READY message
    private boolean isReady = false;

    public String username = null; // clients username

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
     * Checks if the client is ready.
     * 
     * @return true if the client is ready, false otherwise
     */
    public boolean getIsReady() {
        return isReady;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // wait for JOIN message from client
            while (username == null) {
                String line = input.readLine();
                if (line == null) {
                    server.log("Client disconnected before sending username");
                    return;
                }

                if (line.startsWith("JOIN ")) {
                    String proposedUsername = line.substring(5).trim();
                    if (proposedUsername.isEmpty() || server.isUsernameTaken(proposedUsername)) {
                        sendMessage("NAMEINUSE");
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

            // after username is set, wait for READY message
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
                    String fullBase64 = server.encodeCanvasToBase64(server.getServerCanvas());
                    sendMessage("LOADIMG " + fullBase64);

                    // recheck if theres no current turn holder
                    if (server.getCurrentClientTurn() == null) {
                        server.updateTurn();
                    } else {
                        // still broadcast incase order changed
                        server.broadcastUserList();
                    }

                    break;
                }
            }

            // main loop
            String line;
            while ((line = input.readLine()) != null) {
                // if not ready, ignore
                if (!isReady) {
                    continue;
                }

                // ENDTURN
                if (line.equals("ENDTURN")) {
                    // only the current turn holder can end their turn
                    if (server.getCurrentClientTurn() == this) {
                        server.log("Player " + username + " ended their turn.");
                        server.updateTurn();
                    }
                    continue;
                }

                // DRAW
                if (line.startsWith("DRAW ")) {
                    // only current turn holder can draw
                    if (server.getCurrentClientTurn() == this) {
                        // parse and apply
                        String json = line.substring("DRAW ".length());
                        server.applyDrawAction(json);

                        server.sendDrawData(this, line);
                    } else {
                        sendMessage("Not your turn!");
                    }
                    continue;
                }

                // CLEAR
                if (line.equals("CLEAR")) {
                    if (server.getCurrentClientTurn() == this) {
                        server.clearServerCanvas(); // wipe server canvas
                        // send blank canvas to all clients
                        String blankBase64 = server.encodeCanvasToBase64(server.getServerCanvas());
                        server.broadcastMessage("LOADIMG " + blankBase64);
                        server.log("Player " + username + " cleared the canvas.");
                    } else {
                        sendMessage("Not your turn!");
                    }
                    continue;
                }

                // LOADIMG
                if (line.startsWith("LOADIMG ")) {
                    // only current turn holder can load an image
                    if (server.getCurrentClientTurn() == this) {
                        String base64 = line.substring("LOADIMG ".length());
                        server.applyLoadImageAction(base64);
                        server.broadcastMessage(line);
                        server.log("User " + username + " loaded an image.");
                    } else {
                        sendMessage("Not your turn!");
                    }
                    continue;
                }

                // unrecognized message
                server.log("Unknown message from " + username + ": " + line);
            }

        } catch (IOException e) {
            // user disconnected
        } finally {
            closeAll();
            server.removeClient(this);
            server.log("Client disconnected: " + (username != null ? username : socket));
        }
    }

    /**
     * Closes all streams and the socket.
     */
    private void closeAll() {
        try {
            if (input != null)
                input.close();
        } catch (IOException e) {
            // ignore
        }
        if (output != null)
            output.close();
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            // ignore
        }

    }
}
