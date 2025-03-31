package us.otechu.client.ui;

import us.otechu.client.DrawData;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import javax.swing.JPanel;

/**
 * JPanel that manages an offscreen image for drawing
 */
public class DrawingPanel extends JPanel {
    private BufferedImage canvasImage;
    private Graphics2D g2;

    private int prevX, prevY;
    private boolean drawing;
    /** Disables ability to draw when false */
    private boolean drawingEnabled = false;

    // supply the color and thickness
    private Supplier<Color> colorSupplier;
    private Supplier<Integer> thicknessSupplier;

    private DrawTools currentTool;


    public DrawingPanel() {
        // listen for mouse events
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentTool != null && drawingEnabled) {
                    currentTool.onMousePressed(e, g2);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool != null && drawingEnabled) {
                    currentTool.onMouseReleased(e, g2);
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool != null && drawingEnabled) {
                    currentTool.onMouseDragged(e, g2);
                    repaint();
                }
            }
        });

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // if theres no canvas or the window was resized, create a new canvas
//        if (canvasImage == null || canvasImage.getWidth() != getWidth() || canvasImage.getHeight() != getHeight()) {
//            createCanvasImage();
//        }

        if (canvasImage == null) {
          createCanvasImage();
        }

        g.drawImage(canvasImage, 0, 0, null);
        if (currentTool != null) {
            currentTool.preview((Graphics2D) g);
        }
    }

    /**
     * Create or recreate the image
     */
    private void createCanvasImage() {
        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        g2 = canvasImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // smooth lines
        

        // fill the canvas with white
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draws a small dot (if they were to single click)
     */
    private void drawDot(int x, int y) {
        if (g2 != null) {
            g2.setColor(colorSupplier.get());
            int size = thicknessSupplier.get();
            g2.fillOval(x - size / 2, y - size / 2, size, size); // center the dot
            repaint();
        }
    }

    /**
     * Clears the canvas
     */
    public void clearCanvas() {
        if (g2 != null) {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            repaint();
        }
    }

    /**
     * update the drawing attributes
     */
    public void setDrawingAttributes(Supplier<Color> colorSupplier, Supplier<Integer> thicknessSupplier) {
        this.colorSupplier = colorSupplier;
        this.thicknessSupplier = thicknessSupplier;
    }

    /**
     * Access to the image for saving
     */
    public BufferedImage getCanvasImage() {
        return canvasImage;
    }

    /**
     * Sets new image onto the canvas
     * @param image the image to load onto the canvas
     */
    public void setCanvasImage(BufferedImage image) {
        int width = canvasImage.getWidth();
        int height = canvasImage.getHeight();

        // Create blank image with same height/width as canvas
        canvasImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = canvasImage.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // smooth lines
        //Load image onto canvas and refresh
        g2.drawImage(image, 0, 0, null);
        repaint();
    }

    public void setCurrentTool(DrawTools tool) {
        this.currentTool = tool;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
    }
}
