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
    /**  Max number of players */
    public final static int MAX_CLIENTS = 4;

    /** Thread safe list to store draw history - used to update sync canvas of new connections */
    private final List<String> drawHistory = Collections.synchronizedList(new ArrayList<>());

    /** Thread safe list - stores connected clients */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** Index of current players turn (used to index clients list)
     * Shared variable - Ensure thread safety!! */
    private int turnIndex = 0;

    /**
     * Starts server, listens for incoming connections.
     */
    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Drawing server started at port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Reject connection when server is full
                if (clients.size() >= MAX_CLIENTS) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("FULL");
                    clientSocket.close();
                }

                System.out.println("Client Connected");

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
     * Update the client that it's their turn based on the turn index
     */
    public synchronized void setClientTurn() {
        if (clients.isEmpty()) return;

        // Ensure client exists - index desync from d/c
        if (turnIndex > clients.size()) {
            turnIndex = 0;
        }
        this.log("Setting turn for " + turnIndex);
        // Update client
        clients.get(turnIndex).sendMessage("TURN");
    }

    /**
     * Update to next client's turn.
     */
    public synchronized void updateTurn() {
        if (clients.isEmpty()) return;
        int prevTurn = turnIndex;

        // loop through to find next ready player
        for (int i = 1; i <= clients.size(); i++) {
            // Wraps around to first player, after last players turn
            int next = (turnIndex + i) % clients.size();

            if (clients.get(next).getIsReady()) {
                turnIndex = next;
                setClientTurn();
                return;
            }
        }
        // If no one is ready, keep the current turn
        turnIndex = prevTurn;
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
     * @param handler the client handler to remove
     */
    public void removeClient(ClientHandler handler) {
        int clientIndex = clients.indexOf(handler);
        clients.remove(handler);

        if (clients.isEmpty()) {
            turnIndex = 0;
            this.log("No clients connected");
        } else {
            // If it was players turn and they d/c
            if (clientIndex == turnIndex) {
                // Set turn to next player in line
                turnIndex = turnIndex % clients.size();
                setClientTurn();
            // If the player was behind the current turn
            } else if (clientIndex < turnIndex) {
                turnIndex--;
            }

        }
    }
    /**
     * Send a drawing event to all clients except the sender.
     * @param sender the client who sent the drawing event
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
     * TODO: server sided gui?
     * Logs a message to the server console.
     * @param message the message to print
     */
    public void log(String message) {
        System.out.println(message);
    }

    public static  void main(String[] args) {
        new DrawingServer().startServer();
    }
}
