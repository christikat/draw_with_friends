package us.otechu.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The main server for the drawing application.
 * Handles client connections, drawing history, and turn-based drawing control.
 */
public class DrawingServer {
    private final int PORT = 5000;
    /** Max number of players */
    public final static int MAX_CLIENTS = 4;

    /** Thread safe **/
    private final List<String> drawHistory = Collections.synchronizedList(new ArrayList<>()); // list of draw data
                                                                                              // strings
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // list of client handlers
    private final Set<String> activeUsernames = Collections.synchronizedSet(new HashSet<>()); // set of active usernames

    /**
     * Index of current players turn (used to index clients list)
     * Shared variable - Ensure thread safety!!
     */
    private int turnIndex = 0;

    /**
     * Starts server, listens for incoming connections.
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Drawing server started at port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("New connection from " + clientSocket.getRemoteSocketAddress());

                // Reject connection when server is full
                if (clients.size() >= MAX_CLIENTS) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("FULL");
                    clientSocket.close();
                    continue;
                }

                // Create a new client handler for connection and add to client list
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);

                // Start thread for new client
                new Thread(handler, "Handler Thread").start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Checks if the username is already taken.
     * 
     * @param username
     * @return true if username is taken, false otherwise
     */
    public synchronized boolean isUsernameTaken(String username) {
        return activeUsernames.contains(username.toLowerCase());
    }

    /**
     * Adds a username to the active usernames list.
     * 
     * @param username
     */
    public synchronized void addUsername(String username) {
        activeUsernames.add(username.toLowerCase());
    }

    /**
     * Removes a username from the active usernames list.
     * 
     * @param username
     */
    public synchronized void removeUsername(String username) {
        activeUsernames.remove(username.toLowerCase());
    }

    /**
     * Broadcasts the current user list to all clients.
     */
    public synchronized void broadcastUserList() {
        // compile a list of clients usernames
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.username != null) {
                names.add(c.username);
            }
        }
        String listString = String.join(",", names);
        broadcastMessage("USERLIST " + listString);
    }

    /**
     * Update the client that it's their turn based on the turn index
     */
    public synchronized void setClientTurn() {
        if (turnIndex >= clients.size()) {
            turnIndex = 0;
        }
        if (clients.isEmpty()) return; // no clients connected

        // broadcast
        ClientHandler currentClient = clients.get(turnIndex);
        currentClient.sendMessage("TURN"); // notify current client it's their turn
        log("Current turn: " + (currentClient.username != null ? currentClient.username : "index " + turnIndex));
    }

    /**
     * Update to next client's turn.
     */
    public synchronized void updateTurn() {
        if (clients.isEmpty()) return;

        for (int i = 1; i <= clients.size(); i++) {
            int next = (turnIndex + i) % clients.size();
            if (clients.get(next).getIsReady()) {
                turnIndex = next;
                break; // found next player who is ready
            }
        }
        setClientTurn();
    }

    /**
     * Get the client whose turn it is
     */
    public ClientHandler getCurrentClientTurn() {
        if (turnIndex < clients.size()) {
            return clients.get(turnIndex);
        }
        return null;
    }

    /**
     * Removes a client from list, and handles updating turn index
     * 
     * @param handler the client handler to remove
     */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);

        if (handler.username != null) {
            removeUsername(handler.username);
        }

        if (clients.isEmpty()) {
            turnIndex = 0;
            log("No clients connected");
        } else {
            int oldIndex = turnIndex;
            int removedIndex = oldIndex; // IF we find a match

            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i) == handler) {
                    removedIndex = i;
                    break;
                }
            }

            // if the removed client was the current turn player
            if (removedIndex == turnIndex) {
                turnIndex %= clients.size(); // wrap around to first player
                setClientTurn();
            } else if (removedIndex < turnIndex) {
                // If the removed client was before the current turn player
                turnIndex--;
            }
        }

        broadcastUserList(); // broadcast updated user list
    }

    /**
     * Send a drawing event to all clients except the sender.
     * 
     * @param sender   the client who sent the drawing event
     * @param drawData the drawing data to send
     */
    public void sendDrawData(ClientHandler sender, String drawData) {
        // send to all clients except sender
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(drawData);
            }
        }
    }

    /**
     * Returns the current draw history.
     */
    public List<String> getHistory() {
        return drawHistory;
    }

    /**
     * Appends a new draw action to the history.
     * 
     * @param drawData the data of the drawing to add.
     */
    public void updateHistory(String drawData) {
        drawHistory.add(drawData);
    }

    /**
     * Gets the number of connected clients.
     */
    public int getNumClients() {
        return clients.size();
    }

    /**
     * Broadcasts a message to all clients.
     * 
     * @param msg the message to send
     */
    public void broadcastMessage(String msg) {
        for (ClientHandler c : clients) {
            c.sendMessage(msg);
        }
    }

    /**
     * TODO: server sided gui?
     * Logs a message to the server console.
     * 
     * @param message the message to print
     */
    public void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    public static void main(String[] args) {
        new DrawingServer().startServer();
    }
}
