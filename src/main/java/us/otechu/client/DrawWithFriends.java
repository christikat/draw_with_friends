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

    private static void handleServerMessage(String msg) {
        // Notifies player server is full, and closes.
        if (msg.equals("FULL")) {
            JOptionPane.showMessageDialog(null, "Server is currently full. Try again later.");
            System.exit(0);
        }

        // Prevents messages from trying to update the GUI when not ready
        if (!isReady) {
            System.out.println("Client GUI not loaded..");
            return;
        }

        // When player's turn, allow player to draw
        if (msg.equals("TURN")) {
            SwingUtilities.invokeLater(() -> {
                frame.setTurn(true);
            });
        } else if (msg.startsWith("DRAW ")) {
            // handle incoming drawing data from others
            String json = msg.substring(5); // remove "DRAW "
            // Convert json string to DrawData object
            DrawData data = new Gson().fromJson(json, DrawData.class);
            SwingUtilities.invokeLater(() -> {
                // Draws to the canvas
                frame.drawFromData(data);
            });


        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ClientConnection connection = new ClientConnection(DrawWithFriends::handleServerMessage);
            // Create a new instance
                frame = new DrawingAppFrame(connection);
                frame.setVisible(true);

                // Set ready to true when GUI is set up and visible
                isReady = true;

                //Notify the server client is ready for drawing
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