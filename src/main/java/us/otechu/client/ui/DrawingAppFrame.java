package us.otechu.client.ui;

import us.otechu.client.ClientConnection;
import us.otechu.client.DrawData;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * The main app window (JFrame)
 */
public class DrawingAppFrame extends JFrame {

    private DrawingPanel drawingPanel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    // current drawing settings
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;

    private boolean isTurn = false;
    private final ClientConnection connection;
    private JSplitPane splitPane;
    private JButton endTurnButton;
    private JMenuItem openItem;

    private final int COLLAPSED_LIST_LOCATION = 2000; // hides it
    private final int EXPANDED_LIST_LOCATION = 1200;

    public DrawingAppFrame(ClientConnection connection) {
        super("Draw With Friends"); // window title
        this.connection = connection;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 1000);
        setLocationRelativeTo(null); // center window
        setResizable(false); // prevent resizing

        // menu bar for file actions
        createMenuBar();

        // left side content (canvas, color panel etc)
        JPanel leftMainPanel = new JPanel(new BorderLayout());
        JPanel controlPanel = createFloatingControls();
        leftMainPanel.add(controlPanel, BorderLayout.PAGE_START);

        // canvas
        JPanel centerPanel = createCanvasArea();
        leftMainPanel.add(centerPanel, BorderLayout.CENTER);

        // user list
        JScrollPane userListScroll = createUserListScroll();

        // build
        splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftMainPanel,
                userListScroll);

        splitPane.setEnabled(false); // disable resizing
        splitPane.setDividerSize(0); // no visible divider
        splitPane.setDividerLocation(EXPANDED_LIST_LOCATION); // start expanded
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createCanvasArea() {
        JPanel container = new JPanel(new BorderLayout());
        ColourPanel colourPanel = new ColourPanel(this::setCurrentColor);

        // color panel
        JPanel colourContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        colourContainer.add(colourPanel);
        container.add(colourContainer, BorderLayout.LINE_START);

        // drawing panel
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setDrawingAttributes( // supplying the color and thickness
                () -> currentColor,
                () -> brushSize);
        drawingPanel.setCurrentTool(new Pencil(() -> currentColor, () -> brushSize, connection));

        JPanel canvasContainer = new JPanel(new BorderLayout());
        canvasContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        canvasContainer.setBackground(Color.DARK_GRAY);
        canvasContainer.add(drawingPanel, BorderLayout.CENTER);

        container.add(canvasContainer, BorderLayout.CENTER);

        return container;
    }

    /**
     * Creates the floating control panel
     * 
     * @return the control panel
     */
    private JPanel createFloatingControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // left aligned, 10px padding
        panel.setOpaque(false); // transparent
        panel.setBounds(10, 10, 400, 50); // top-left

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

        // pencil
        JButton pencilButton = new JButton("Pencil");
        pencilButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Pencil(() -> currentColor, () -> brushSize, connection)));
        panel.add(pencilButton);

        // line
        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Line(() -> currentColor, () -> brushSize, connection)));
        panel.add(lineButton);

        // rectangle
        JButton rectButton = new JButton("Rectangle");
        rectButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Rectangle(() -> currentColor, () -> brushSize, connection)));
        panel.add(rectButton);

        // end turn
        endTurnButton = new JButton("End Turn");
        endTurnButton.setEnabled(false);

        endTurnButton.addActionListener(e -> {
            // Prevent sending when it's not their turn
            if (!isTurn)
                return;

            setTurn(false);
            if (connection != null) {
                connection.send("ENDTURN");
            }
        });
        panel.add(endTurnButton);

        // toggle user list
        JButton toggleListButton = new JButton("Hide Players List");
        toggleListButton.addActionListener(e -> {
            if (splitPane.getDividerLocation() == EXPANDED_LIST_LOCATION) {
                splitPane.setDividerLocation(COLLAPSED_LIST_LOCATION);
                toggleListButton.setText("Show Players List");
            } else {
                splitPane.setDividerLocation(EXPANDED_LIST_LOCATION);
                toggleListButton.setText("Hide Players List");
            }
        });
        panel.add(toggleListButton);

        return panel;
    }

    /**
     * Menu bar for file actions
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openImage());
        openItem.setEnabled(false); // disabled until it's our turn
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
     * A collapsible user list panel to show active users
     */
    private JScrollPane createUserListScroll() {
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);

        // allow horizontal scrolling
        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setMinimumSize(new Dimension(50, getHeight()));
        return scrollPane;
    }

    /**
     * [SERVER] Updates the user list with the current active users
     * 
     * @param names
     */
    public void updateUserList(String[] names) {
        userListModel.clear();
        for (String name : names) {
            userListModel.addElement(name);
        }
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
                JOptionPane.showMessageDialog(this, "Saved to " + file.getAbsolutePath(), "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving file: " + exception.getMessage(), "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Opens an image and loads it onto the canvas
     */
    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open an Image");

        int userChoice = fileChooser.showOpenDialog(this); // "this" is the parent window
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    drawingPanel.setCanvasImage(img); // set local canvas

                    String base64 = encodeToBase64(img);
                    connection.send("LOADIMG " + base64); // send to server
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Encodes a BufferedImage to a Base64 string
     * 
     * @param image the image to encode
     * @return the Base64 encoded string
     * @throws IOException
     */
    private String encodeToBase64(BufferedImage image) throws IOException {
        // easteregg: first thing imma load is a trackhawk... source Big30
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public void loadImageFromBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage img = ImageIO.read(bais);
            if (img != null) {
                drawingPanel.setCanvasImage(img);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Takes the data from a drawing action and displays it on the canvas
     * 
     * @param data the drawing data to display
     */
    public void drawFromData(DrawData data) {
        Graphics2D g2 = drawingPanel.getCanvasImage().createGraphics();

        // Set colour and thickness
        g2.setColor(Color.decode(data.colourHex));
        g2.setStroke(new BasicStroke(data.thickness));

        // Use the appropriate method for each shape
        switch (data.shape) {
            case "pencil":
            case "line":
                g2.drawLine(data.x1, data.y1, data.x2, data.y2);
                break;
            case "rect":
                int rx = Math.min(data.x1, data.x2);
                int ry = Math.min(data.y1, data.y2);
                int rw = Math.abs(data.x2 - data.x1);
                int rh = Math.abs(data.y2 - data.y1);
                g2.drawRect(rx, ry, rw, rh);
                break;
            default:
                System.out.println("Error: invalid shape - " + data.shape);
                break;
        }

        // Draws the line from data coordinates
//        g2.drawLine(data.x1, data.y1, data.x2, data.y2);
        drawingPanel.repaint();
    }

    private void setCurrentColor(Color color) {
        currentColor = color;
    }

    /**
     * Updates the players turn, enabling/disabling drawing
     * 
     * @param isTurn true if it's their turn
     */
    public void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
        drawingPanel.setDrawingEnabled(isTurn);
        // disable end turn button when not their turn
        endTurnButton.setEnabled(isTurn);
        openItem.setEnabled(isTurn); // only can open a image if its your turn

        if (isTurn) {
            JOptionPane.showMessageDialog(this, "It's your turn to draw!");
        }
    }
}
