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
    private final ClientConnection connection;
    private final String localUsername; // who the user is

    private DrawingPanel drawingPanel;

    // current drawing settings
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;

    private boolean isTurn = false;

    private JButton endTurnButton;
    private JMenuItem openItem;

    // player list panels
    private JPanel playersPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private boolean playersListVisible = true;

    // track indexes from server
    private int currentIndex = -1;
    private int nextIndex = -1;

    public DrawingAppFrame(ClientConnection connection, String localUsername) {
        super("Draw With Friends"); // window title
        this.connection = connection;
        this.localUsername = localUsername;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 1000);
        setLocationRelativeTo(null); // center window
        setResizable(true);
       

        createMenuBar(); // file actions
        initLayout(); // main layout
    }

    /**
     * Creates the main layout of the app
     */
    private void initLayout() {
        setLayout(new BorderLayout());

        JPanel topControls = createTopControls();
        add(topControls, BorderLayout.NORTH);

        // the center is the color panel + drawing panel
        JPanel centerPanel = createCanvasArea();
        add(centerPanel, BorderLayout.CENTER);

        // right side -> players list
        playersPanel = createPlayersPanel();
        add(playersPanel, BorderLayout.EAST);
    }

    /**
     * Creates the control panel for the drawing canvas.
     * 
     * @return the drawing panel
     */
    private JPanel createTopControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // color button
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Choose Brush Color", currentColor);
            if (chosen != null) {
                currentColor = chosen;
            }
        });
        panel.add(colorButton);

        // thickness spinner
        panel.add(new JLabel("Thickness"));
        JSpinner thicknessSpinner = new JSpinner(new SpinnerNumberModel(brushSize, 1, 50, 1));
        thicknessSpinner.addChangeListener(e -> brushSize = (int) thicknessSpinner.getValue());
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

        // end turn
        endTurnButton = new JButton("End Turn");
        endTurnButton.setEnabled(false);
        endTurnButton.addActionListener(e -> {
            if (!isTurn)
                return;
            setTurn(false);
            connection.send("ENDTURN");
        });
        panel.add(endTurnButton);

        // toggle player list
        JButton toggleListButton = new JButton("Hide Players List");
        toggleListButton.addActionListener(e -> {
            playersListVisible = !playersListVisible;
            if (playersListVisible) {
                // re-add the panel to the layout
                add(playersPanel, BorderLayout.EAST);
                toggleListButton.setText("Hide Players List");
            } else {
                // remove the panel from the layout
                remove(playersPanel);
                toggleListButton.setText("Show Players List");
            }
            
            revalidate();
            repaint();
        });
        panel.add(toggleListButton);

        return panel;
    }

    /**
     * Color Palate & Drawing Panel
     * 
     * @return JPanel with the drawing canvas and color palate
     */
    private JPanel createCanvasArea() {
        JPanel container = new JPanel(new BorderLayout());

        // color panel on the left
        ColourPanel colourPanel = new ColourPanel(c -> currentColor = c);
        JPanel colourContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        colourContainer.add(colourPanel);
        container.add(colourContainer, BorderLayout.WEST);

        // the drawing panel in the center
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setDrawingAttributes(() -> currentColor, () -> brushSize);
        drawingPanel.setCurrentTool(new Pencil(() -> currentColor, () -> brushSize, connection));

        // add some margin around it
        JPanel canvasContainer = new JPanel(new BorderLayout());
        canvasContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        canvasContainer.add(drawingPanel, BorderLayout.CENTER);

        container.add(canvasContainer, BorderLayout.CENTER);

        return container;
    }

    /**
     * Creates the players list panel
     * 
     * @return
     */
    private JPanel createPlayersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Players List", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(titleLabel, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new PlayerListRenderer(() -> currentIndex, () -> nextIndex, () -> localUsername));

        JScrollPane scrollPane = new JScrollPane(userList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // set a preferred width
        panel.setPreferredSize(new Dimension(250, getHeight()));

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
     * Called by the server whenever we get
     * "USERLIST name1,name2,name3|current|next"
     * We'll parse out the name list + the indexes.
     */
    public void updateUserList(String userListMessage, String localUser) {
        String[] parts = userListMessage.split("\\|");
        // parts[0] => "name1,name2,name3"
        // parts[1] => currentIndex as string
        // parts[2] => nextIndex as string

        // parse user names
        String[] names = parts[0].split(",");
        currentIndex = parseIndex(parts[1]);
        nextIndex = parseIndex(parts[2]);

        // update the model
        userListModel.clear();
        for (String name : names) {
            userListModel.addElement(name);
        }
    }

    /**
     * Parses the index from the string
     * @param s the string to parse
     * @return the index, or -1 if invalid
     */
    private int parseIndex(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1; // invalid index
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

        // Draws the line from data coordinates
        g2.drawLine(data.x1, data.y1, data.x2, data.y2);
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
