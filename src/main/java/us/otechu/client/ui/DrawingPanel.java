package us.otechu.client.ui;

import us.otechu.client.DrawData;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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

    /** Disables ability to draw when false */
    private boolean drawingEnabled = false;

    // supply the color and thickness
    private Supplier<Color> colorSupplier;
    private Supplier<Integer> thicknessSupplier;
    private DrawTools currentTool;

    public DrawingPanel() {
        // setup
        setDoubleBuffered(true);
        setBackground(Color.WHITE);

        // listen for mouse events
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (canDraw()) {
                    currentTool.onMousePressed(e, g2);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (canDraw()) {
                    currentTool.onMouseReleased(e, g2);
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (canDraw()) {
                    currentTool.onMouseDragged(e, g2);
                    repaint();
                }
            }
        });

        // listen for resizing events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeCanvasIfNeeded();
            }
        });

    }

    /**
     * Determines if we can draw on the canvas
     * 
     * @return true if we can draw, false otherwise
     */
    private boolean canDraw() {
        return drawingEnabled && currentTool != null && g2 != null;
    }

    /**
     * On first paint (or if the image is null), create the canvas.
     * Then draw the image onto the panel's Graphics.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvasImage == null) {
            createInitialCanvas();
        }
        g.drawImage(canvasImage, 0, 0, null);

        if (currentTool != null) {
            currentTool.preview((Graphics2D) g);
        }
    }

    /**
     * Creates the initial canvas image and fills it with white.
     */
    private void createInitialCanvas() {
        // create the image
        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        g2 = canvasImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Resizes the image if the panel is bigger than the current image.
     * Preserves existing drawings by copying the old image into the new one.
     */
    private void resizeCanvasIfNeeded() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0)
            return; // invalid size

        // if we have no image yet, or the panel grew beyond the current canvas
        if (canvasImage == null ||
                canvasImage.getWidth() < w ||
                canvasImage.getHeight() < h) {

            // new bigger image
            BufferedImage newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D gNew = newImg.createGraphics();
            gNew.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // fill background white
            gNew.setColor(Color.WHITE);
            gNew.fillRect(0, 0, w, h);

            // copy old content
            if (canvasImage != null) {
                gNew.drawImage(canvasImage, 0, 0, null);
                g2.dispose(); // discard old
            }

            canvasImage = newImg;
            g2 = canvasImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
     * Sets new image onto the canvas
     * 
     * @param image the image to load onto the canvas
     */
    public void setCanvasImage(BufferedImage image) {
        int w = canvasImage.getWidth();
        int h = canvasImage.getHeight();

        // create new image
        canvasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        g2 = canvasImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // fill new image white, then draw the server image in full
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        g2.drawImage(image, 0, 0, null);
        repaint();
    }

    /**
     * Access to the image for saving
     */
    public BufferedImage getCanvasImage() {
        return canvasImage;
    }

    public void setCurrentTool(DrawTools tool) {
        this.currentTool = tool;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
    }
}
