package us.otechu.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
/**
 * Colour Palette JPanel to quickly select colours
 */
public class ColourPanel extends JPanel {
    // Colour palette colors
    // TODO add more custom colours
    private final Color[] colours = {
            Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
            Color.CYAN, Color.GRAY, Color.DARK_GRAY, Color.MAGENTA, Color.ORANGE,
            Color.PINK, Color.YELLOW,
    };

    public ColourPanel(Consumer<Color> colourConsumer) {
        // single column of colours
        setLayout(new GridLayout(0, 1, 5, 5));

        // loop to create each colour button
        for (Color colour : colours) {
            JButton colourButton = new JButton();
            colourButton.setBackground(colour);
            colourButton.setPreferredSize(new Dimension(25, 25));
            colourButton.addActionListener(e -> colourConsumer.accept(colour));
            add(colourButton);
        }
    }
}
