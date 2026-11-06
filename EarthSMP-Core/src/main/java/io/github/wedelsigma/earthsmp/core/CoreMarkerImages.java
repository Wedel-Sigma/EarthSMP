package io.github.wedelsigma.earthsmp.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** An original, procedurally drawn lodestone-style pixel icon with ALWAYS-VISIBLE XYZ label.
 * No external images or player data are fetched from the squaremap web server.
 */
final class CoreMarkerImages {
    private static final Font COORD_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final int TOP = 24;
    private static final int ICON_SIZE = 30;
    private static final int HEIGHT = TOP + ICON_SIZE + 3;
    private static final int PADDING = 12;

    private CoreMarkerImages() { }

    static String label(int x, int y, int z) {
        return "X: " + x + "  Y: " + y + "  Z: " + z;
    }

    static BufferedImage create(int x, int y, int z) {
        String label = label(x, y, z);
        // Measure with an offscreen image to preserve enough width even for negative coordinates.
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metric = measure.createGraphics();
        metric.setFont(COORD_FONT);
        int width = Math.max(ICON_SIZE + 2, metric.getFontMetrics().stringWidth(label) + PADDING * 2);
        metric.dispose();

        BufferedImage out = new BufferedImage(width, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(COORD_FONT);
            FontMetrics fm = g.getFontMetrics();
            // An opaque dark plate keeps the coordinates readable against any map biome.
            g.setColor(new Color(9, 18, 25, 228));
            g.fillRoundRect(1, 1, width - 2, 20, 8, 8);
            g.setColor(new Color(182, 198, 205));
            g.drawRoundRect(1, 1, width - 3, 19, 8, 8);
            g.setColor(Color.WHITE);
            g.drawString(label, (width - fm.stringWidth(label)) / 2, 15);
            drawLodestone(g, (width - ICON_SIZE) / 2, TOP);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static void drawLodestone(Graphics2D g, int x, int y) {
        // Original 2D game-style icon: stone face, dark corner frame, metallic concentric ring.
        g.setColor(new Color(2, 8, 13, 150));
        g.fillRoundRect(x + 2, y + 3, 28, 28, 4, 4);
        g.setColor(new Color(40, 54, 61));
        g.fillRect(x + 1, y, 28, 28);
        g.setColor(new Color(91, 112, 121));
        g.fillRect(x + 3, y + 2, 24, 23);
        g.setColor(new Color(146, 160, 164));
        g.fillRect(x + 5, y + 3, 20, 19);
        g.setColor(new Color(25, 36, 44));
        g.fillRect(x + 7, y + 5, 16, 16);
        g.setColor(new Color(98, 119, 129));
        g.fillRect(x + 10, y + 7, 10, 12);
        g.setColor(new Color(184, 192, 186));
        g.fillRect(x + 12, y + 9, 6, 8);
        g.setColor(new Color(47, 64, 69));
        g.fillRect(x + 14, y + 10, 2, 6);
        g.setColor(new Color(29, 40, 46));
        g.fillRect(x + 5, y + 23, 20, 4);
        g.setColor(new Color(105, 126, 133));
        g.fillRect(x + 3, y + 24, 2, 3);
        g.fillRect(x + 25, y + 24, 2, 3);
        g.setColor(new Color(214, 227, 225));
        g.setStroke(new BasicStroke(1));
        g.drawRect(x + 1, y, 27, 27);
    }
}
