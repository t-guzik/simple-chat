package app;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AsciiArt {
    private static final int WIDTH = 144;
    private static final int HEIGHT = 32;

    public static String getArt() throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("Serif", Font.PLAIN, 20));

        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString("J A V A", 1, 20);
        StringBuilder art = new StringBuilder();

        for (int y = 0; y < HEIGHT; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < WIDTH; x++)
                sb.append(image.getRGB(x, y) == -16777216 ? " " : "|");

            if (sb.toString().trim().isEmpty())
                continue;

            art.append(sb);
            art.append("\n");
        }
        //System.out.println(art.toString());
        return art.toString();
    }
}