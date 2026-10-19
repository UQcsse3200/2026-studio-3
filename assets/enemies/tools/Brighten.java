import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Brightens enemy frames that are too dark: lifts and cools the shadows, keeps glowing parts
 * unchanged, then adds a cool rim light along the inside of the outline.
 *
 * <p>Usage: java Brighten.java &lt;gamma&gt; &lt;rim_strength&gt; &lt;out_dir&gt; &lt;frame.png&gt; [&lt;frame.png&gt;
 * ...]
 *
 * <p>gamma: below 1, smaller is brighter, e.g. 0.65; rim_strength: rim light strength from 0 to 1,
 * e.g. 0.45
 */
public class Brighten {
  static final double STEEL_HUE = 0.6; // Cool hue to shift towards (blue-grey)
  static final double STEEL_MIN_SAT = 0.15; // Minimum cool tint given to grey parts
  static final int[] RIM_COLOR = {150, 185, 215};
  static final int RIM_WIDTH = 5; // Rim light width (pixels on a 512 frame)

  public static void main(String[] args) throws IOException {
    double gamma = Double.parseDouble(args[0]);
    double rim = Double.parseDouble(args[1]);
    File outDir = new File(args[2]);
    outDir.mkdirs();
    for (int i = 3; i < args.length; i++) {
      File in = new File(args[i]);
      BufferedImage frame = read(in);
      ImageIO.write(brighten(frame, gamma, rim), "png", new File(outDir, in.getName()));
      System.out.println("brightened " + in.getName());
    }
  }

  static BufferedImage brighten(BufferedImage frame, double gamma, double rimStrength) {
    int w = frame.getWidth();
    int h = frame.getHeight();
    int[] px = frame.getRGB(0, 0, w, h, null, 0, w);
    for (int i = 0; i < px.length; i++) {
      int a = px[i] >>> 24;
      int r = (px[i] >> 16) & 0xFF;
      int g = (px[i] >> 8) & 0xFF;
      int b = px[i] & 0xFF;
      if (a == 0 || isGlow(r, g, b)) {
        continue;
      }
      double[] hsv = rgbToHsv(r / 255.0, g / 255.0, b / 255.0);
      double hh = hsv[0];
      double s = hsv[1];
      double v = Math.pow(hsv[2], gamma);
      if (s < 0.25) {
        hh = STEEL_HUE;
        s = Math.max(s, STEEL_MIN_SAT);
      }
      double[] c = hsvToRgb(hh, s, v);
      px[i] = argb(a, Math.rint(c[0] * 255), Math.rint(c[1] * 255), Math.rint(c[2] * 255));
    }

    // The band just inside the outline: original alpha minus the eroded alpha
    boolean[] solid = new boolean[px.length];
    for (int i = 0; i < px.length; i++) {
      solid[i] = (px[i] >>> 24) > 128;
    }
    int reach = RIM_WIDTH / 2;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (!solid[y * w + x] || shrunk(solid, w, h, x, y, reach)) {
          continue;
        }
        int c = px[y * w + x];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        if (isGlow(r, g, b)) {
          continue;
        }
        double k = rimStrength;
        px[y * w + x] =
            argb(
                c >>> 24,
                Math.rint(r * (1 - k) + RIM_COLOR[0] * k),
                Math.rint(g * (1 - k) + RIM_COLOR[1] * k),
                Math.rint(b * (1 - k) + RIM_COLOR[2] * k));
      }
    }
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    out.setRGB(0, 0, w, h, px, 0, w);
    return out;
  }

  /**
   * Whether this pixel is still inside the shape after erosion (minimum filter); positions
   * outside the image use the nearest edge pixel.
   */
  private static boolean shrunk(boolean[] solid, int w, int h, int x, int y, int reach) {
    for (int dy = -reach; dy <= reach; dy++) {
      for (int dx = -reach; dx <= reach; dx++) {
        int sx = Math.min(w - 1, Math.max(0, x + dx));
        int sy = Math.min(h - 1, Math.max(0, y + dy));
        if (!solid[sy * w + sx]) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean isGlow(int r, int g, int b) {
    int hi = Math.max(r, Math.max(g, b));
    int lo = Math.min(r, Math.min(g, b));
    return hi > 200 || (hi > 140 && (hi - lo) / (double) hi > 0.55);
  }

  /** Same as Python colorsys.rgb_to_hsv. */
  static double[] rgbToHsv(double r, double g, double b) {
    double hi = Math.max(r, Math.max(g, b));
    double lo = Math.min(r, Math.min(g, b));
    if (hi == lo) {
      return new double[] {0.0, 0.0, hi};
    }
    double range = hi - lo;
    double rc = (hi - r) / range;
    double gc = (hi - g) / range;
    double bc = (hi - b) / range;
    double h = r == hi ? bc - gc : g == hi ? 2.0 + rc - bc : 4.0 + gc - rc;
    h = (h / 6.0) % 1.0;
    if (h < 0) {
      h += 1.0;
    }
    return new double[] {h, range / hi, hi};
  }

  /** Same as Python colorsys.hsv_to_rgb. */
  static double[] hsvToRgb(double h, double s, double v) {
    if (s == 0.0) {
      return new double[] {v, v, v};
    }
    int i = (int) (h * 6.0);
    double f = h * 6.0 - i;
    double p = v * (1.0 - s);
    double q = v * (1.0 - s * f);
    double t = v * (1.0 - s * (1.0 - f));
    switch (Math.floorMod(i, 6)) {
      case 0:
        return new double[] {v, t, p};
      case 1:
        return new double[] {q, v, p};
      case 2:
        return new double[] {p, v, t};
      case 3:
        return new double[] {p, q, v};
      case 4:
        return new double[] {t, p, v};
      default:
        return new double[] {v, p, q};
    }
  }

  private static int argb(int a, double r, double g, double b) {
    return a << 24 | (int) r << 16 | (int) g << 8 | (int) b;
  }

  private static BufferedImage read(File f) throws IOException {
    BufferedImage bi = ImageIO.read(f);
    if (bi == null) {
      throw new IOException("unsupported image format: " + f);
    }
    return bi;
  }
}
