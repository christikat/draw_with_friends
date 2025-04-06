package us.otechu.server;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

import jdk.jshell.execution.Util;
import us.otechu.client.DrawData;
import us.otechu.common.Utils;

/**
 * The main server for the drawing application.
 * Handles client connections, drawing history, and turn-based drawing control.
 */
public class DrawingServer {
    private final int PORT = 5000;
    /** Max number of players */
    public final static int MAX_CLIENTS = 4;

    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private BufferedImage serverCanvas;
    private Graphics2D serverG2;

    // thread safe collections
    private final List<String> drawHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Set<String> activeUsernames = Collections.synchronizedSet(new HashSet<>());

    // the index of the client whose turn it is
    // -1 means no one is playing
    private int turnIndex = -1;

    public DrawingServer() {
        // create a single big image in memory
        serverCanvas = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
        serverG2 = serverCanvas.createGraphics();
        serverG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // fill it white
        serverG2.setColor(Color.WHITE);
        serverG2.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    }

    /**
     * Clears the server canvas by filling it with white.
     */
    public void clearServerCanvas() {
        // fill the entire region white
        serverG2.setColor(Color.WHITE);
        serverG2.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    }

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
     * Applies a drawing action to the server canvas.
     * 
     * @param drawDataJson the JSON string containing drawing data
     */
    public void applyDrawAction(String drawDataJson) {
        // parse the JSON into (x1, y1, x2, y2, color, thickness)
        DrawData data = parseDrawJson(drawDataJson);

        // then draw onto serverCanvas
        Utils.drawFromData(serverG2, data);
    }

    /**
     * Applies a load image action to the server canvas.
     * 
     * @param base64 the base64 encoded image string
     */
    public void applyLoadImageAction(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage loaded = ImageIO.read(new ByteArrayInputStream(bytes));
            if (loaded != null) {
                serverG2.drawImage(loaded, 0, 0, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the server canvas image.
     * 
     * @return the server canvas image
     */
    public BufferedImage getServerCanvas() {
        return serverCanvas;
    }

    /**
     * Encodes the server canvas image to a base64 string.
     * 
     * @param img the image to encode
     * @return the base64 encoded string
     */
    public String encodeCanvasToBase64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Parses the JSON string into a DrawData object.
     * 
     * @param json the JSON string to parse
     * @return the parsed DrawData object
     */
    private DrawData parseDrawJson(String json) {
        return new Gson().fromJson(json, DrawData.class);
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
     * Broadcasts the current players list to all clients as well as the server's
     * turn index.
     */
    public synchronized void broadcastUserList() {
        // create the list of usernames
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.username != null) {
                names.add(c.username);
            } else {
                names.add("joining..."); // unknown clients
            }
        }
        String nameString = String.join(",", names);

        // current turn index
        int current = turnIndex; // could be -1

        // next index
        int next = -1;
        if (current != -1 && !clients.isEmpty()) {
            int size = clients.size();
            // search for next available player
            for (int i = 1; i <= size; i++) {
                int candidate = (current + i) % size;
                ClientHandler ch = clients.get(candidate);
                if (ch.username != null && ch.getIsReady()) {
                    next = candidate;
                    break;
                }
            }
        }

        broadcastMessage("USERLIST " + nameString + "|" + current + "|" + next);
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
            if (current.username != null) {
                broadcastMessage("LOG It's " + current.username + " turn!");
            }

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
                broadcastUserList(); // update player list
                return;
            }
        }

        // if we get here, no ready clients exist
        turnIndex = -1;
        broadcastUserList(); // no valid turn holder
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

        broadcastUserList(); // broadcast updated players list
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
