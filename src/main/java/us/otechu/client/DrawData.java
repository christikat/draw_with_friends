package us.otechu.client;
import java.awt.Color;

/**
 * An object to hold the data from a drawing action
 * Data includes coordinates, stroke thickness, colour.
 */
public class DrawData {
    public int x1, y1, x2, y2;
    public int thickness;
    public String colourHex;

    /**
     * Constructs a DrawData object for a drawing action.
     *
     * @param x1        Starting x coordinate
     * @param y1        Starting y coordinate
     * @param x2        Ending x coordinate
     * @param y2        Ending y coordinate
     * @param colour    The color of the stroke
     * @param thickness The stroke thickness
     */
    public DrawData(int x1, int y1, int x2, int y2, Color colour, int thickness) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.colourHex = colorToHex(colour);
        this.thickness = thickness;
    }

    /**
     * Converts a Java Color object to a hexadecimal string.
     *
     * @param color The Color to convert
     * @return Hex string of the color
     */
    public static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }
}
