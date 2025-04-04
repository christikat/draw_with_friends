package us.otechu.client;

import com.google.gson.Gson;
import us.otechu.client.ui.DrawingAppFrame;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * DrawWithFriends
 */
public class DrawWithFriends extends JFrame {
    // true when client gui is ready for server messages
    private static boolean isReady = false;
    private static DrawingAppFrame frame;
    private static ClientConnection connection;

    private static volatile String nameResult = ""; // used to check if name is taken
    private static String localUsername = ""; 

    private static void handleServerMessage(String msg) {
        // Notifies player server is full, and closes.
        if (msg.equals("FULL")) {
            JOptionPane.showMessageDialog(null, "Server is currently full. Try again later.");
            System.exit(0);
        }

        // if server rejects username, re-prompt or close
        if (msg.equals("NAMEINUSE")) {
            nameResult = "NAMEINUSE";
            return;
        }

        // if server accepts username, notify client
        if (msg.startsWith("JOINED ")) {
            nameResult = "JOINED";
            localUsername = msg.substring(7).trim(); // get username from server
            return;
        }

        // parse user list, drawing data, etc.
        if (msg.startsWith("USERLIST ")) {
            String data = msg.substring("USERLIST ".length());
            if (frame != null) {
                SwingUtilities.invokeLater(() -> frame.updateUserList(data, localUsername));
            }
        } else if (msg.startsWith("DRAW ")) {
            String json = msg.substring(5);
            DrawData drawData = new Gson().fromJson(json, DrawData.class);
            if (frame != null) {
                SwingUtilities.invokeLater(() -> frame.drawFromData(drawData));
            }
        } else if (msg.startsWith("LOADIMG ")) {
            // base64 image
            String base64 = msg.substring(8);
            if (frame != null) {
                SwingUtilities.invokeLater(() -> frame.loadImageFromBase64(base64));
            }
        } else if (msg.equals("TURN")) {
            if (frame != null) {
                SwingUtilities.invokeLater(() -> frame.setTurn(true));
            }
        } else if (msg.equals("CLEAR")) {
            SwingUtilities.invokeLater(() -> frame.clear());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                connection = new ClientConnection(DrawWithFriends::handleServerMessage);

                // repeatedly prompt for username until success or user quits
                while (true) {
                    nameResult = "";
                    String username = JOptionPane.showInputDialog(null,
                            "Enter username:", "Login",
                            JOptionPane.QUESTION_MESSAGE);
                    if (username == null || username.trim().isEmpty()) {
                        System.exit(0);
                    }
                    connection.send("JOIN " + username.trim());

                    // wait up to 5 seconds for either NAMEINUSE or JOINED
                    boolean done = false;
                    for (int i = 0; i < 50; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt(); // restore interrupted status
                        }
                        if (nameResult.equals("NAMEINUSE")) {
                            JOptionPane.showMessageDialog(null,
                                    "Username already in use. Please try again.",
                                    "Duplicate Username",
                                    JOptionPane.WARNING_MESSAGE);
                            break; // break the for loop, re-prompt
                        } else if (nameResult.equals("JOINED")) {
                            done = true; // success
                            break;
                        }
                    }
                    if (done)
                        break;
                }

                // Create a new instance
                frame = new DrawingAppFrame(connection, localUsername);
                frame.setVisible(true);

                // Set ready to true when GUI is set up and visible
                isReady = true;
                connection.send("READY");

                // Handle disconnect when window closes
                frame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        connection.disconnect();
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}