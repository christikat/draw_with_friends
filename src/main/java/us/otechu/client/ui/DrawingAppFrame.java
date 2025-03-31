package us.otechu.client.ui;

import us.otechu.client.ClientConnection;
import us.otechu.client.DrawData;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The main app window (JFrame)
 */
public class DrawingAppFrame extends JFrame {

    private DrawingPanel drawingPanel;
    private JPanel controlPanel;

    // current drawing settings
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;
    private DrawTools currentTool;
    JButton endTurnButton;
    private boolean isTurn = false;
    private final ClientConnection connection;

    public DrawingAppFrame(ClientConnection connection) {
        super("Draw With Friends"); // window title
        this.connection = connection;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 1000);
        setLocationRelativeTo(null); // center window
        setResizable(false); // prevent resizing

        // menu bar for file actions
        createMenuBar();

        // create the canvas
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        // supply for color and thickness
        drawingPanel.setDrawingAttributes(
            () -> currentColor,
            () -> brushSize
        );
        drawingPanel.setCurrentTool(new Pencil(()-> currentColor, ()-> brushSize, connection));

        // canvas container to set boarder
        JPanel canvasContainer = new JPanel(new BorderLayout());
        canvasContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        canvasContainer.setBackground(Color.DARK_GRAY);
        canvasContainer.add(drawingPanel, BorderLayout.CENTER);
        add(canvasContainer, BorderLayout.CENTER);

        //Add colour palette panel on the left
        ColourPanel colourPanel = new ColourPanel(this::setCurrentColor); // function to update colour
        JPanel colourContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        colourContainer.add(colourPanel);
        add(colourContainer, BorderLayout.LINE_START);

        // create the control panel
        controlPanel = createFloatingControls();
        add(controlPanel, BorderLayout.PAGE_START);
    }

    /**
     * Creates the floating control panel
     * @return the control panel
     * TODO: Make them able to move around
     */
    private JPanel createFloatingControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // left aligned, 10px padding
        panel.setOpaque(false);                   // transparent
        panel.setBounds(10, 10, 400 ,50); // top-left

        // color picker
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Choose Brush Color", currentColor);
            if (chosen != null) {
                currentColor = chosen;
            }
        });
        panel.add(colorButton);


        // thickness chooser
        panel.add(new JLabel("Thickness"));
        JSpinner thicknessSpinner = new JSpinner(new SpinnerNumberModel(brushSize, 1, 50, 1));
        thicknessSpinner.addChangeListener(e -> brushSize = (int) thicknessSpinner.getValue()); // update brush size
        panel.add(thicknessSpinner);

        // clear canvas
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> drawingPanel.clearCanvas());
        panel.add(clearButton);


        JButton pencilButton = new JButton("Pencil");
        pencilButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Pencil(()-> currentColor, ()-> brushSize, connection)));
        panel.add(pencilButton);

        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Line(()-> currentColor, ()-> brushSize, connection)));
        panel.add(lineButton);

        endTurnButton = new JButton("End Turn");
        endTurnButton.setEnabled(false);

        endTurnButton.addActionListener(e -> {
            // Prevent sending when it's not their turn
            if (!isTurn) return;

            setTurn(false);
            if (connection != null) {
                connection.send("ENDTURN");
            }
        });
        panel.add(endTurnButton);


        return panel;
    }

    /**
     * Menu bar for file actions
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem  = new JMenuItem("Open");
        openItem.addActionListener(e->openImage());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveDrawing());
        fileMenu.add(saveItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    /**
     * Save the drawing to a file
     */
    private void saveDrawing() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Drawing");
        fileChooser.setSelectedFile(new File("drawing.png")); // default file name

        int userChoice = fileChooser.showSaveDialog(this); // "this" is the parent window
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedImage image = drawingPanel.getCanvasImage();
                ImageIO.write(image, "png", file);
                JOptionPane.showMessageDialog(this, "Saved to " + file.getAbsolutePath(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving file: " + exception.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    /**
     * Opens an image and loads it onto the canvas
     */
    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        int userChoice = fileChooser.showOpenDialog(this); // "this" is the parent window

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                //add image to canvas
                drawingPanel.setCanvasImage(ImageIO.read(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Takes the data from a drawing action and displays it on the canvas
     * @param data the drawing data to display
     */
    public void drawFromData(DrawData data) {
        Graphics2D g2 = drawingPanel.getCanvasImage().createGraphics();

        // Set colour and thickness
        g2.setColor(Color.decode(data.colourHex));
        g2.setStroke(new BasicStroke(data.thickness));

        // Draws the line from data coordinates
        g2.drawLine(data.x1, data.y1, data.x2, data.y2);
        drawingPanel.repaint();
    }


    private void setCurrentColor(Color color) {
        currentColor = color;
    }
    /**
     * Updates the players turn, enabling/disabling drawing
     * @param isTurn true if it's their turn
     */
    public void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
        drawingPanel.setDrawingEnabled(isTurn);
        // disable end turn button when not their turn
        endTurnButton.setEnabled(isTurn);
        if (isTurn) {
            JOptionPane.showMessageDialog(this, "It's your turn to draw!");
        }
    }
}
