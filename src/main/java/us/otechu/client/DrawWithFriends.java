package us.otechu.client;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import us.otechu.client.ui.DrawingAppFrame;


/**
 * DrawWithFriends
 */
public class DrawWithFriends extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create a new instance
            DrawingAppFrame frame = new DrawingAppFrame();
            frame.setVisible(true);
        });
    }
}