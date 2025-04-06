package us.otechu.client.ui;

import com.google.gson.Gson;
import us.otechu.client.ClientConnection;
import us.otechu.client.DrawData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

/**
 * Interface for drawing tools.
 * Methods to handle mouse events
 */
public interface DrawTools {
    void onMousePressed(MouseEvent e, Graphics2D g2);
    void onMouseDragged(MouseEvent e, Graphics2D g2);
    void onMouseReleased(MouseEvent e, Graphics2D g2);
    void preview(Graphics2D g2);
}
/**
 * Tool for freehand drawing when dragging mouse.
 */
class Pencil implements DrawTools {
    private int prevX, prevY;
    private final Supplier<Color> colorSupplier;
    private final Supplier<Integer> thicknessSupplier;
    private final ClientConnection connection;

    public Pencil(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier, ClientConnection connection) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
        this.connection = connection;
    }

    @Override
    public void onMousePressed(MouseEvent e, Graphics2D g2) {
        prevX = e.getX();
        prevY = e.getY();
        // Draws a dot when only clicked
        drawDot(prevX, prevY, g2);
    }

    @Override
    public void onMouseDragged(MouseEvent e, Graphics2D g2) {
        int x = e.getX();
        int y = e.getY();
        // Set brush settings
        g2.setColor(colorSupplier.get());
        g2.setStroke(new BasicStroke(thicknessSupplier.get()));

        // Draws small line segement
        g2.drawLine(prevX, prevY, x, y);


        // Send data for new line to the server to update other players
        DrawData data = new DrawData(prevX, prevY, x, y, colorSupplier.get(), thicknessSupplier.get(), "pencil", false);
        String json = new Gson().toJson(data);
        connection.send("DRAW " + json);

        prevX = x;
        prevY = y;
    }

    @Override
    public void onMouseReleased(MouseEvent e, Graphics2D g) {
        // Nothing to override
    }

    @Override
    public void preview(Graphics2D g2) {
        //Nothing to override
    }

    /**
     * Draws a dot at the location clicked
     */
    private void drawDot(int x, int y, Graphics2D g) {
        g.setColor(colorSupplier.get());
        int size = thicknessSupplier.get();
        g.fillOval(x - size / 2, y - size / 2, size, size);
    }
}
/**
 * Tool for drawing lines when dragging mouse.
 */
class Line implements  DrawTools {
    private int x1, y1, x2, y2;
    private boolean isDragging = false;
    private final Supplier<Color> colorSupplier;
    private final Supplier<Integer> thicknessSupplier;
    private final ClientConnection connection;

    public Line(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier, ClientConnection connection) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
        this.connection = connection;
    }

    @Override
    public void onMousePressed(MouseEvent e, Graphics2D g2) {
        // Set all coords when first clicked
        x1 = e.getX();
        y1 = e.getY();
        x2 = e.getX();
        y2 = e.getY();
        isDragging = true;
    }

    @Override
    public void onMouseDragged(MouseEvent e, Graphics2D g2) {
        // Update final coords during draggin
        x2 = e.getX();
        y2 = e.getY();
    }

    @Override
    public void onMouseReleased(MouseEvent e, Graphics2D g2) {
        g2.setStroke(new BasicStroke(thicknessSupplier.get()));
        g2.setColor(colorSupplier.get());
        // Draw the final line when released
        g2.drawLine(x1, y1, x2, y2);
        isDragging = false;

        // Send data for new line to the server to update other players
        DrawData data = new DrawData(x1, y1, x2, y2, colorSupplier.get(), thicknessSupplier.get(), "line", false);
        String json = new Gson().toJson(data);
        connection.send("DRAW " + json);
    }
    /**
     * Draws a preview of the line while dragging.
     */
    public void preview(Graphics2D g2) {
        if (isDragging) {
            g2.setStroke(new BasicStroke(thicknessSupplier.get()));
            g2.setColor(colorSupplier.get());
            g2.drawLine(x1, y1, x2, y2);
        }
    }
}
/**
 * Tool for drawing rectangles, click and drag
 */
class Rectangle implements DrawTools {
    private int x1, y1, x2, y2;
    private boolean isDragging = false;
    private final Supplier<Color> colorSupplier;
    private final Supplier<Integer> thicknessSupplier;
    Supplier<Boolean> filled;
    private final ClientConnection connection;

    Rectangle(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier, Supplier<Boolean> filled, ClientConnection connection) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
        this.filled = filled;
        this.connection = connection;
    }

    @Override
    public void onMousePressed(MouseEvent e, Graphics2D g2) {
        x1 = e.getX();
        y1 = e.getY();
        x2 = e.getX();
        y2 = e.getY();
        isDragging = true;
    }

    @Override
    public void onMouseDragged(MouseEvent e, Graphics2D g2) {
        x2 = e.getX();
        y2 = e.getY();
    }

    @Override
    public void onMouseReleased(MouseEvent e, Graphics2D g2) {
        isDragging = false;
        g2.setStroke(new BasicStroke(thicknessSupplier.get()));
        g2.setColor(colorSupplier.get());

        // Get the top left corner (x,y)
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);

        //Make sure width and height are positive
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);

        if (filled.get()) {
            g2.fillRect(x, y, width, height);
        } else {
            g2.drawRect(x, y, width, height);
        }

        DrawData data = new DrawData(x, y, x + width, y + height, colorSupplier.get(), thicknessSupplier.get(), "rect", filled.get());
        String json = new Gson().toJson(data);
        connection.send("DRAW " + json);
    }

    @Override
    public void preview(Graphics2D g2) {
        if (isDragging) {
            g2.setStroke(new BasicStroke(thicknessSupplier.get()));
            g2.setColor(colorSupplier.get());

            int x = Math.min(x1, x2);
            int y = Math.min(y1, y2);
            int width = Math.abs(x2 - x1);
            int height = Math.abs(y2 - y1);


            if (filled.get()) {
                g2.fillRect(x, y, width, height);
            } else {
                g2.drawRect(x, y, width, height);
            }
        }
    }
}

/**
 * Tool for drawing circles/ovals, click and drag.
 */
class Circle implements DrawTools {
    private int x1, y1, x2, y2;
    private boolean isDragging = false;
    private final Supplier<Color> colorSupplier;
    private final Supplier<Integer> thicknessSupplier;
    Supplier<Boolean> filled;
    private final ClientConnection connection;

    Circle(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier, Supplier<Boolean> filled, ClientConnection connection) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
        this.filled = filled;
        this.connection = connection;
    }

    @Override
    public void onMousePressed(MouseEvent e, Graphics2D g2) {
        x1 = e.getX();
        y1 = e.getY();
        x2 = x1;
        y2 = y1;
        isDragging = true;
    }

    @Override
    public void onMouseDragged(MouseEvent e, Graphics2D g2) {
        x2 = e.getX();
        y2 = e.getY();
    }

    @Override
    public void onMouseReleased(MouseEvent e, Graphics2D g2) {
        isDragging = false;
        g2.setStroke(new BasicStroke(thicknessSupplier.get()));
        g2.setColor(colorSupplier.get());

        // Get top left of the coordinates
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);

        //Get positive height/width
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);


        if (filled.get()) {
            g2.fillOval(x, y, width, height);
        } else {
            g2.drawOval(x, y, width, height);
        }

        DrawData data = new DrawData(x, y, x + width, y + height, colorSupplier.get(), thicknessSupplier.get(), "circle", filled.get());
        String json = new Gson().toJson(data);
        connection.send("DRAW " + json);
    }

    @Override
    public void preview(Graphics2D g2) {
        if (isDragging) {
            g2.setStroke(new BasicStroke(thicknessSupplier.get()));
            g2.setColor(colorSupplier.get());

            int x = Math.min(x1, x2);
            int y = Math.min(y1, y2);
            int width = Math.abs(x2 - x1);
            int height = Math.abs(y2 - y1);

            if (filled.get()) {
                g2.fillOval(x, y, width, height);
            } else {
                g2.drawOval(x, y, width, height);
            }
        }
    }
}

/**
 * Tool for writing text onto the canvas
 */
class TextTool implements DrawTools {
    private final Supplier<Color> colorSupplier;
    private final Supplier<Integer> thicknessSupplier;
    private final ClientConnection connection;
    private final Window parent;

    public TextTool(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier, ClientConnection connection, Window parent) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
        this.connection = connection;
        this.parent = parent;
    }

    @Override
    public void onMousePressed(MouseEvent e, Graphics2D g2) {
        // Pop up to get user input
        String input = JOptionPane.showInputDialog(parent, "Enter text:");
        if (input != null && !input.isEmpty()) {
            g2.setColor(colorSupplier.get());
            // Text size based on thickness supplier
            g2.setFont(new Font("Arial", Font.PLAIN, thicknessSupplier.get() * 5));
            g2.drawString(input, e.getX(), e.getY());

            DrawData data = new DrawData(e.getX(), e.getY(), 0, 0, colorSupplier.get(), thicknessSupplier.get(), "text:" + input, false);
            String json = new Gson().toJson(data);
            connection.send("DRAW " + json);
        }
    }

    @Override
    public void onMouseDragged(MouseEvent e, Graphics2D g2) {}

    @Override
    public void onMouseReleased(MouseEvent e, Graphics2D g2) {}

    @Override
    public void preview(Graphics2D g2) {}
}