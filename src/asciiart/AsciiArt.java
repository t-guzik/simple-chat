package asciiart;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AsciiArt {
    public static String getArt() throws IOException {
        int width = 45;
        int height = 20;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("SansSerif", Font.BOLD, 14));

        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString("JAVA", 10, 20);
        StringBuilder art = new StringBuilder();

        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++)
                sb.append(image.getRGB(x, y) == -16777216 ? " " : "$");

            if (sb.toString().trim().isEmpty())
                continue;

            art.append(sb);
            art.append("\n");
        }
        return art.toString();
    }
}