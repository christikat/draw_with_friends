package us.otechu.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
/**
 * Colour Palette JPanel to quickly select colours
 */
public class ColourPanel extends JPanel {
    // Colour palette colors
    private final Color[] colours = {
            // Grayscale
            Color.BLACK,
            Color.DARK_GRAY,
            Color.GRAY,
            Color.LIGHT_GRAY,
            new Color(230, 230, 230),
            Color.WHITE,

            // Reds
            new Color(139, 0, 0),
            Color.RED,
            new Color(255, 102, 102),
            new Color(255, 192, 203),
            new Color(255, 105, 180),
            new Color(255, 20, 147),

            // Oranges
            new Color(255, 69, 0),
            Color.ORANGE,
            new Color(255, 165, 0),
            new Color(255, 140, 0),
            new Color(255, 200, 124),
            new Color(255, 239, 213),

            // Yellows
            Color.YELLOW,
            new Color(255, 255, 102),
            new Color(255, 255, 153),
            new Color(255, 250, 205),
            new Color(240, 230, 140),
            new Color(238, 232, 170),

            // Greens
            new Color(0, 100, 0),
            Color.GREEN,
            new Color(0, 255, 0),
            new Color(144, 238, 144),
            new Color(152, 251, 152),
            new Color(60, 179, 113),

            // Cyans/Teals
            new Color(0, 139, 139),
            Color.CYAN,
            new Color(175, 238, 238),
            new Color(0, 128, 128),
            new Color(64, 224, 208),
            new Color(224, 255, 255),

            // Blues
            new Color(0, 0, 139),
            Color.BLUE,
            new Color(30, 144, 255),
            new Color(135, 206, 250),
            new Color(173, 216, 230),
            new Color(0, 191, 255),

            // Purples
            new Color(75, 0, 130),
            new Color(128, 0, 128),
            new Color(186, 85, 211),
            Color.MAGENTA,
            new Color(221, 160, 221),
            new Color(238, 130, 238),

            // Browns
            new Color(139, 69, 19),
            new Color(160, 82, 45),
            new Color(210, 180, 140),
            new Color(222, 184, 135),
            new Color(244, 164, 96),
            new Color(205, 133, 63)
    };

    public ColourPanel(Consumer<Color> colourConsumer) {
        // 2 columns of colours
        setLayout(new GridLayout(0, 2, 2, 2));

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
