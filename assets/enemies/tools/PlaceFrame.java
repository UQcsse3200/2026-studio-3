import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Scales a transparent image whose background is already removed and places it in a 512x512 frame,
 * aligned with the existing frames.
 *
 * <p>Usage: java PlaceFrame.java &lt;in.png&gt; &lt;out.png&gt; &lt;height&gt; &lt;x&gt; &lt;y&gt; [--center]
 *
 * <ul>
 *   <li>height: height of the content (the opaque part) after scaling, in frame pixels
 *   <li>x, y: by default where the bottom centre of the content goes (for standing enemies, y is
 *       usually the ground line 478); with --center, where the content's centre goes (floating
 *       enemies)
 * </ul>
 */
public class PlaceFrame {
  static final int FRAME = 512;
  static final int ALPHA_MIN = 16; // Faint glow below this alpha does not count as content

  public static void main(String[] args) throws IOException {
    BufferedImage in = ImageIO.read(new File(args[0]));
    if (in == null) {
      throw new IOException("unsupported image format: " + args[0]);
    }
    int height = Integer.parseInt(args[2]);
    double x = Double.parseDouble(args[3]);
    double y = Double.parseDouble(args[4]);
    boolean center = args.length > 5 && args[5].equals("--center");

    int[] box = contentBox(in);
    double scale = (double) height / (box[3] - box[1]);
    double contentW = (box[2] - box[0]) * scale;
    double left = x - contentW / 2;
    double top = center ? y - height / 2.0 : y - height;

    // Scale the whole image by one ratio, then move the content box's top-left to the target
    BufferedImage out = new BufferedImage(FRAME, FRAME, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.translate(left - box[0] * scale, top - box[1] * scale);
    g.scale(scale, scale);
    g.drawImage(in, 0, 0, null);
    g.dispose();
    ImageIO.write(out, "png", new File(args[1]));

    int[] placed = contentBox(out);
    System.out.printf(
        "placed %s: scale %.2f, content (%d,%d)-(%d,%d)%n",
        new File(args[1]).getName(), scale, placed[0], placed[1], placed[2], placed[3]);
    if (placed[0] <= 0 || placed[1] <= 0 || placed[2] >= FRAME || placed[3] >= FRAME) {
      System.out.println("  warning: content touches the frame edge and may be cut off");
    }
  }

  /** Bounding box of the opaque content {left, top, right, bottom}, right and bottom exclusive. */
  static int[] contentBox(BufferedImage img) {
    int x0 = img.getWidth();
    int y0 = img.getHeight();
    int x1 = 0;
    int y1 = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (img.getRGB(x, y) >>> 24 >= ALPHA_MIN) {
          x0 = Math.min(x0, x);
          y0 = Math.min(y0, y);
          x1 = Math.max(x1, x + 1);
          y1 = Math.max(y1, y + 1);
        }
      }
    }
    return new int[] {x0, y0, x1, y1};
  }
}
