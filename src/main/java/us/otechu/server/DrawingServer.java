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

    // thread safe collections
    private final List<String> drawHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Set<String> activeUsernames = Collections.synchronizedSet(new HashSet<>());

    // the index of the client whose turn it is
    // -1 means no one is playing
    private int turnIndex = -1;

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
                new Thread(handler, "ClientHandler Thread").start();
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
     * Notifies the client at turnIndex that it is their turn.
     */
    public synchronized void setClientTurn() {
        if (turnIndex < 0 || turnIndex >= clients.size()) {
            turnIndex = -1;
            return;
        }
        ClientHandler current = clients.get(turnIndex);
        if (current != null) {
            current.sendMessage("TURN");
            log("Current turn: " + (current.username != null ? current.username : ("index " + turnIndex)));
        }
    }

    /**
     * Update to next client's turn.
     */
    public synchronized void updateTurn() {
        // no clients? no turns.
        if (clients.isEmpty()) {
            turnIndex = -1;
            return;
        }

        // ensure its within bounds
        if (turnIndex < 0 || turnIndex >= clients.size()) {
            turnIndex = 0;
        }

        // move to next client
        turnIndex = (turnIndex + 1) % clients.size();

        // find fully connected player
        int size = clients.size();
        for (int i = 0; i < size; i++) {
            int candidate = (turnIndex + i) % size;
            ClientHandler ch = clients.get(candidate);

            if (ch.username != null && ch.getIsReady()) {
                turnIndex = candidate;
                setClientTurn(); // notify its their turn
                return;
            }
        }

        // if we get here, no ready clients exist
        turnIndex = -1;
    }

    /**
     * Get the client whose turn it is
     */
    public ClientHandler getCurrentClientTurn() {
        // return null if turnIndex is invalid
        if (turnIndex < 0 || turnIndex >= clients.size()) {
            return null;
        }
        return clients.get(turnIndex);
    }

    /**
     * Removes a client from the server, and updates turnIndex if necessary.
     * 
     * @param handler the client handler to remove
     */
    public void removeClient(ClientHandler handler) {
        // find them first
        int removedIndex = clients.indexOf(handler);
        if (removedIndex == -1)
            return; // not found

        clients.remove(handler);
        if (handler.username != null) {
            removeUsername(handler.username);
        }

        if (clients.isEmpty()) {
            turnIndex = -1;
            log("No clients connected");
        } else {
            // if the removed client was the turn holder
            if (removedIndex == turnIndex) {
                // we'll pick the next available or -1 if none
                turnIndex = -1;
                updateTurn();
            } else if (removedIndex < turnIndex) {
                // if a client before the current turn was removed, shift turnIndex back by 1
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
     * Sends message to clients to clear canvas
     */
    public void clearCanvas(ClientHandler sender) {
        // send to all clients except sender
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage("CLEAR");
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

    public void clearHistory() { drawHistory.clear(); }

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
