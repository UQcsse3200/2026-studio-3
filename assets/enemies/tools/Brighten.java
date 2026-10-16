import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 提亮过暗的敌人帧：暗部提亮并偏冷色，发光部分保持不变，再沿轮廓内侧加一圈冷色边缘光。
 *
 * <p>用法: java Brighten.java &lt;gamma&gt; &lt;rim_strength&gt; &lt;out_dir&gt; &lt;frame.png&gt; [&lt;frame.png&gt;
 * ...]
 *
 * <p>gamma: 小于 1 越小越亮，比如 0.65；rim_strength: 边缘光强度 0~1，比如 0.45
 */
public class Brighten {
  static final double STEEL_HUE = 0.6; // 偏向的冷色色相（蓝灰）
  static final double STEEL_MIN_SAT = 0.15; // 灰色部分至少带这么多冷色
  static final int[] RIM_COLOR = {150, 185, 215};
  static final int RIM_WIDTH = 5; // 边缘光宽度（512 帧上的像素）

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

    // 轮廓内侧的一圈：原 alpha 减去收缩后的 alpha
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

  /** 收缩（最小值滤波）后这个像素是否仍在形状内；越界的位置取最近的边缘像素。 */
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

  /** 和 Python colorsys.rgb_to_hsv 相同。 */
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

  /** 和 Python colorsys.hsv_to_rgb 相同。 */
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
