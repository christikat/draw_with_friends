package us.otechu.client.ui;

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

    public Pencil(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
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

    public Line(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
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
        g2.drawLine(x1, y1, e.getX(), e.getY());
        isDragging = false;
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