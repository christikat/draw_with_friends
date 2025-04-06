package us.otechu.client.ui;

import java.awt.Color;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.function.Supplier;

/**
 * A custom list cell renderer to color each entry based on:
 * - Current player => Green
 * - Next player => Yellow
 * - Local player => Gray
 * - Everyone else => Black
 *
 * If the local player is also current or next, the local user color is overridden
 * by green or yellow.
 */
class PlayerListRenderer extends DefaultListCellRenderer {
    private final Supplier<Integer> currentIndexSupplier;
    private final Supplier<Integer> nextIndexSupplier;
    private final Supplier<String> localUserSupplier;

    public PlayerListRenderer(Supplier<Integer> currentIndexSupplier,
            Supplier<Integer> nextIndexSupplier,
            Supplier<String> localUserSupplier) {
        this.currentIndexSupplier = currentIndexSupplier;
        this.nextIndexSupplier = nextIndexSupplier;
        this.localUserSupplier = localUserSupplier;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        String name = (value == null) ? "" : value.toString();
        int current = currentIndexSupplier.get();
        int next = nextIndexSupplier.get();
        String local = localUserSupplier.get();

        Color textColor = Color.BLACK;

        if (index == current) {
            textColor = new Color(0x2ECC71);
        }

        else if (index == next) {
            textColor = new Color(0xF1C40F);
        }

        else if (name.equalsIgnoreCase(local)) {
            textColor = new Color(0xE74C3C);
        }

        setForeground(textColor);
        setText(name);

        return c;
    }
}
