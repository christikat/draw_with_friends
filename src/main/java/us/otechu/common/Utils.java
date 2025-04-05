package us.otechu.common;

import us.otechu.client.DrawData;

import java.awt.*;

public class Utils {
    public static void drawFromData(Graphics2D g2, DrawData data) {
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
                if (data.filled) {
                    g2.fillRect(rx, ry, rw, rh);
                } else {
                    g2.drawRect(rx, ry, rw, rh);
                }
                break;
            case "circle":
                int cx = Math.min(data.x1, data.x2);
                int cy = Math.min(data.y1, data.y2);
                int cw = Math.abs(data.x2 - data.x1);
                int ch = Math.abs(data.y2 - data.y1);
                if (data.filled) {
                    g2.fillOval(cx, cy, cw, ch);
                } else {
                    g2.drawOval(cx, cy, cw, ch);
                }
                break;
            default:
                System.out.println("Error: invalid shape - " + data.shape);
                break;
        }
    }
}
