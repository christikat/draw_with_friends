package us.otechu.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The main app window (JFrame)
 */
public class DrawingAppFrame extends JFrame {
    
    private JLayeredPane layeredPane;
    private DrawingPanel drawingPanel;
    private JPanel controlPanel;

    // current drawing settings
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;

    public DrawingAppFrame() {
        super("Draw With Friends"); // window title
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000,700); // window size
        setLocationRelativeTo(null); // center window

        // menu bar for file actions
        createMenuBar();

        // create the layered pane
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null); // we manually position components
        add(layeredPane, BorderLayout.CENTER);

        // create the canvas
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setBounds(0, 0, getWidth(), getHeight()); // fill the window if they resize
        // supply for color and thickness
        drawingPanel.setDrawingAttributes(
            () -> currentColor,
            () -> brushSize
        );
        layeredPane.add(drawingPanel, JLayeredPane.DEFAULT_LAYER);

        // create the control panel
        controlPanel = createFloatingControls();
        layeredPane.add(controlPanel, JLayeredPane.PALETTE_LAYER);

        // handle window resizing
        addComponentListener(new ComponentAdapter() {
            @Override // override the method to handle the event
            public void componentResized(ComponentEvent e) {
                // resize the canvas to fill the window
                drawingPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            }
        });
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
    
        return panel;
    }

    /**
     * Menu bar for file actions
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

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

}
