package us.otechu.client.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import us.otechu.client.ClientConnection;
import us.otechu.client.DrawData;
import us.otechu.common.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private JButton clearButton;
    private JMenuItem openItem;

    // player list panels
    private JPanel playersPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private boolean playersListVisible = true;

    private JTextArea gameLogs;
    private JTextField chatInput;

    // track indexes from server
    private int currentIndex = -1;
    private int nextIndex = -1;

    // Variables for button icons
    private static final Color ICON_COLOUR = new Color(0x4D8BFF);
    private static final int ICON_SIZE = 24;

    private static final String FONT = "SansSerif";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    public DrawingAppFrame(ClientConnection connection, String localUsername) {
        super("Draw With Friends"); // window title
        this.connection = connection;
        this.localUsername = localUsername;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 1000);
        setLocationRelativeTo(null); // center window
        setResizable(true);

        try {
//            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            SwingUtilities.updateComponentTreeUI(this); // update the UI
        } catch (Exception e) {
            e.printStackTrace();
        }


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
        playersPanel = createSidePanel();
        add(playersPanel, BorderLayout.EAST);
    }

    /**
     * Creates the control panel for the drawing canvas.
     * 
     * @return the drawing panel
     */
    private JPanel createTopControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Brush Settings - custom colour, thickness, fill
        JPanel brushSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        brushSettings.setBorder(BorderFactory.createTitledBorder("Brush Settings"));

        // color button
        JButton colorButton = new JButton("Colour", loadIcon("colour.png"));
        colorButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Choose Brush Colour", currentColor);
            if (chosen != null) {
                currentColor = chosen;
            }
        });
        brushSettings.add(colorButton);

        // thickness spinner
        brushSettings.add(new JLabel("Thickness"));
        JSpinner thicknessSpinner = new JSpinner(new SpinnerNumberModel(brushSize, 1, 50, 1));
        thicknessSpinner.addChangeListener(e -> brushSize = (int) thicknessSpinner.getValue());
        brushSettings.add(thicknessSpinner);

        JCheckBox fillCheckBox = new JCheckBox("Fill");
        brushSettings.add(fillCheckBox);

        panel.add(brushSettings);

        // Tools - pencil, line, rect, circle, text
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        toolPanel.setBorder(BorderFactory.createTitledBorder("Tools"));

        // pencil
        JButton pencilButton = new JButton("Pencil", loadIcon("pencil.png"));
        pencilButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Pencil(() -> currentColor, () -> brushSize, connection)));
        toolPanel.add(pencilButton);

        // line
        JButton lineButton = new JButton("Line", loadIcon("line.png"));
        lineButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Line(() -> currentColor, () -> brushSize, connection)));
        toolPanel.add(lineButton);

        // rectangle
        JButton rectButton = new JButton("Rectangle", loadIcon("rect.png"));
        rectButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Rectangle(() -> currentColor, () -> brushSize, () -> fillCheckBox.isSelected(), connection)));
        toolPanel.add(rectButton);

        // circle
        JButton circleButton = new JButton("Circle", loadIcon("circle.png"));
        circleButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new Circle(() -> currentColor, () -> brushSize, () -> fillCheckBox.isSelected(), connection)));
        toolPanel.add(circleButton);

        // text
        JButton textButton = new JButton("Text", loadIcon("text.png"));
        textButton.addActionListener(e -> drawingPanel.setCurrentTool(
                new TextTool(() -> currentColor, () -> brushSize, connection)));
        toolPanel.add(textButton);

        panel.add(toolPanel);

        // canvas
        JPanel canvasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        canvasPanel.setBorder(BorderFactory.createTitledBorder("Canvas"));

        // clear canvas
        clearButton = new JButton("Clear", loadIcon("clear.png"));
        clearButton.setEnabled(false);
        clearButton.addActionListener(e -> {
            if (!isTurn) return;
            connection.send("CLEAR");
        });
        canvasPanel.add(clearButton);

        // end turn
        endTurnButton = new JButton("End Turn", loadIcon("end.png"));
        endTurnButton.setEnabled(false);
        endTurnButton.addActionListener(e -> {
            if (!isTurn) return;
            setTurn(false);
            connection.send("ENDTURN");
        });
        canvasPanel.add(endTurnButton);

        panel.add(canvasPanel);

        // Panel for UI toggles - theme/player list
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        togglePanel.setBorder(BorderFactory.createTitledBorder("UI"));

        // Toggle between dark and light
        JToggleButton toggleTheme = new JToggleButton("Dark Mode", loadIcon("dark.png"));
        toggleTheme.addActionListener(e -> {
            try {
                if (toggleTheme.isSelected()) {
                    // Set dark mode
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    toggleTheme.setText("Light Mode");
                    toggleTheme.setIcon(loadIcon("light.png"));
                } else {
                    // Set light mode
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    toggleTheme.setText("Dark Mode");
                    toggleTheme.setIcon(loadIcon("dark.png"));
                }

                // Repaint all windows to apply the new theme
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        togglePanel.add(toggleTheme);

        // toggle player list
        JButton toggleListButton = new JButton("Hide Players List", loadIcon("player.png"));
        toggleListButton.addActionListener(e -> {
            playersListVisible = !playersListVisible;
            if (playersListVisible) {
                add(playersPanel, BorderLayout.EAST);
                toggleListButton.setText("Hide Players List");
            } else {
                remove(playersPanel);
                toggleListButton.setText("Show Players List");
            }
            revalidate();
            repaint();
        });
        togglePanel.add(toggleListButton);

        panel.add(togglePanel);

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

        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Players List",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font(FONT, Font.BOLD, 12)
        ));
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
     * Creates the side panel for list of players and logs
     */
    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        playersPanel = createPlayersPanel();

        // Create a panel for game logs and chat
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Drawing Logs",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font(FONT, Font.BOLD, 12)
        ));

        // Text area for logs
        gameLogs = new JTextArea();
        gameLogs.setEditable(false);
        gameLogs.setLineWrap(true);
        gameLogs.setWrapStyleWord(true);
        gameLogs.setRows(20);
        gameLogs.setFont(new Font(FONT, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(gameLogs);
        gameLogs.append("Welcome to Drawing with Friends \uD83D\uDE04\n");
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel for chat input and send
        JPanel chatPanel = new JPanel(new BorderLayout());
        // Send message on "enter"
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendChatMessage());

        // Send messge on button click
        JButton sendButton = new JButton("", loadIcon("send.png"));
        sendButton.addActionListener(e -> sendChatMessage());

        chatPanel.add(chatInput, BorderLayout.CENTER);
        chatPanel.add(sendButton, BorderLayout.EAST);
        logPanel.add(chatPanel, BorderLayout.SOUTH);

        sidePanel.add(playersPanel, BorderLayout.CENTER);
        sidePanel.add(logPanel, BorderLayout.PAGE_END);
        return sidePanel;
    }

    private void sendChatMessage() {
        String msg = chatInput.getText();
        if (!msg.trim().isEmpty()) {
            connection.send("CHAT " + msg);
            chatInput.setText("");
        }
    }

    /**
     * Appends a message to the logs with a timestamp
     * @param text the text to add
     */
    public void updateLog(String text) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        gameLogs.append("[" + timestamp + "] " + text + "\n");

        //Auto scroll
        gameLogs.setCaretPosition(gameLogs.getDocument().getLength());
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
//                    String base64 = encodeToBase64(drawingPanel.getCanvasImage());
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
        Utils.drawFromData(g2, data);
        drawingPanel.repaint();
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
        clearButton.setEnabled(isTurn);
        openItem.setEnabled(isTurn); // only can open a image if its your turn

        if (isTurn) {
            JOptionPane.showMessageDialog(this, "It's your turn to draw!");
        }
    }

    /**
     * Creates a sized and coloured icon from an image file
     *
     * @param filename the name of the image file
     */
    public static ImageIcon loadIcon(String filename) {
        String path = "/icons/" + filename;
        URL resource = Objects.requireNonNull(DrawingAppFrame.class.getResource(path), "Icon not found: " + path);

        // Get icon and image from png
        ImageIcon rawIcon = new ImageIcon(resource);
        Image rawImage = rawIcon.getImage();

        // Create resized image
        BufferedImage scaledImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dScaled = scaledImage.createGraphics();
        // Smooth the image with bi-linear interpolation (good for icons)
        g2dScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2dScaled.drawImage(rawImage, 0, 0, ICON_SIZE, ICON_SIZE, null);
        g2dScaled.dispose();

        // Tint the image
        BufferedImage tintedImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tintedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);

        // Only colour the non-transparent pixels
        g2d.setComposite(AlphaComposite.SrcAtop);
        g2d.setColor(ICON_COLOUR);
        g2d.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
        g2d.dispose();

        return new ImageIcon(tintedImage);
    }

}

