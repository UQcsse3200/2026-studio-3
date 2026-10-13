import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * 白底 AI 图抠图：去白底/水印/被包围的白色空隙，光晕做半透明"去白"并保持明亮。
 *
 * <p>用法: java Cutout.java &lt;in.png&gt; &lt;out.png&gt; [选项]
 *
 * <ul>
 *   <li>--watermark x0,y0,x1,y1 | none：用背景色盖掉的水印区域，默认 1262,1440,1536,1536
 *   <li>--min-gap N：被身体围住的白色空隙超过这么多像素才去掉，默认 300
 *   <li>--bg-tol N：和背景色每个通道相差不超过这个值才算"纯背景"，默认 3
 *   <li>--alpha-min F：去白后不透明度低于这个值的像素直接去掉，默认 0.03
 *   <li>--grey-sat F：背景区域里饱和度低于它的灰色像素（影子、灰雾、灰色速度线）直接去掉，有颜色的光效保留
 *   <li>--floor-y N：这一行以下，较深的灰色（地面影子）也算背景；遇到角色的深色描边会停下，不会吃进灰白色的靴子
 * </ul>
 *
 * <p>只能读 JDK 支持的格式（PNG/JPEG 等），.webp 原图要先另存为 PNG。
 */
public class Cutout {
  public static void main(String[] args) throws IOException {
    int[] watermark = {1262, 1440, 1536, 1536};
    int minGap = 300;
    int bgTol = 3;
    double alphaMin = 0.03;
    Double greySat = null;
    Integer floorY = null;
    for (int i = 2; i < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case "--watermark":
          watermark =
              value.equals("none")
                  ? null
                  : Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
          break;
        case "--min-gap":
          minGap = Integer.parseInt(value);
          break;
        case "--bg-tol":
          bgTol = Integer.parseInt(value);
          break;
        case "--alpha-min":
          alphaMin = Double.parseDouble(value);
          break;
        case "--grey-sat":
          greySat = Double.parseDouble(value);
          break;
        case "--floor-y":
          floorY = Integer.parseInt(value);
          break;
        default:
          throw new IllegalArgumentException("unknown option " + args[i]);
      }
    }
    BufferedImage in = ImageIO.read(new File(args[0]));
    if (in == null) {
      throw new IOException("unsupported image format: " + args[0]);
    }
    ImageIO.write(
        cutout(in, watermark, minGap, bgTol, alphaMin, greySat, floorY), "png", new File(args[1]));
    System.out.println("cut out " + args[1]);
  }

  static BufferedImage cutout(
      BufferedImage im,
      int[] watermark,
      int minGap,
      int bgTol,
      double alphaMin,
      Double greySat,
      Integer floorY) {
    int w = im.getWidth();
    int h = im.getHeight();
    int[] px = im.getRGB(0, 0, w, h, null, 0, w);
    for (int i = 0; i < px.length; i++) {
      px[i] &= 0xFFFFFF;
    }
    // 用四周边缘像素的中位数当作背景色
    int edgeCount = 0;
    int[][] edge = new int[3][2 * ((w + 7) / 8) + 2 * ((h + 7) / 8)];
    for (int x = 0; x < w; x += 8) {
      for (int y : new int[] {0, h - 1}) {
        addChannels(edge, edgeCount++, px[y * w + x]);
      }
    }
    for (int y = 0; y < h; y += 8) {
      for (int x : new int[] {0, w - 1}) {
        addChannels(edge, edgeCount++, px[y * w + x]);
      }
    }
    int[] bg = new int[3];
    for (int c = 0; c < 3; c++) {
      int[] values = Arrays.copyOf(edge[c], edgeCount);
      Arrays.sort(values);
      bg[c] = values[edgeCount / 2];
    }
    int bgRgb = bg[0] << 16 | bg[1] << 8 | bg[2];
    if (watermark != null) {
      for (int y = Math.max(0, watermark[1]); y < Math.min(h, watermark[3]); y++) {
        for (int x = Math.max(0, watermark[0]); x < Math.min(w, watermark[2]); x++) {
          px[y * w + x] = bgRgb;
        }
      }
    }
    double[] sat = new double[px.length];
    double[] val = new double[px.length];
    for (int i = 0; i < px.length; i++) {
      int r = (px[i] >> 16) & 0xFF;
      int g = (px[i] >> 8) & 0xFF;
      int b = px[i] & 0xFF;
      double hi = Math.max(r, Math.max(g, b)) / 255.0;
      double lo = Math.min(r, Math.min(g, b)) / 255.0;
      val[i] = hi;
      sat[i] = hi == lo ? 0.0 : (hi - lo) / hi;
    }

    Region light =
        i -> {
          int y = i / w;
          if (floorY != null && y >= floorY && sat[i] < 0.12 && val[i] > 0.42) {
            return true;
          }
          return val[i] > 0.80 && sat[i] < 0.45;
        };
    // 被身体围住的大块背景色（与外部不连通）；只认和背景色几乎相同的像素，避免误删白色光芯
    Region pureWhite =
        i -> {
          for (int c = 0; c < 3; c++) {
            if (Math.abs(((px[i] >> (16 - 8 * c)) & 0xFF) - bg[c]) > bgTol) {
              return false;
            }
          }
          return true;
        };

    int[] border = new int[2 * w + 2 * h];
    int n = 0;
    for (int x = 0; x < w; x++) {
      border[n++] = x;
      border[n++] = (h - 1) * w + x;
    }
    for (int y = 0; y < h; y++) {
      border[n++] = y * w;
      border[n++] = y * w + w - 1;
    }
    boolean[] reg = flood(w, h, border, n, light);

    boolean[] done = new boolean[w * h];
    int[] gapSeeds = new int[w * h];
    int seedCount = 0;
    for (int y = 0; y < h; y += 3) {
      for (int x = 0; x < w; x += 3) {
        int i = y * w + x;
        if (reg[i] || done[i] || !pureWhite.test(i)) {
          continue;
        }
        boolean[] comp = flood(w, h, new int[] {i}, 1, j -> !reg[j] && pureWhite.test(j));
        int size = 0;
        for (boolean b : comp) {
          size += b ? 1 : 0;
        }
        for (int j = 0; j < comp.length; j++) {
          if (comp[j]) {
            done[j] = true;
            if (size > minGap) {
              gapSeeds[seedCount++] = j;
            }
          }
        }
      }
    }
    // 从空隙往外扩展到相邻的浅色过渡像素，去掉空隙边缘的白色毛边
    if (seedCount > 0) {
      boolean[] grown = flood(w, h, gapSeeds, seedCount, j -> light.test(j) || pureWhite.test(j));
      for (int j = 0; j < grown.length; j++) {
        reg[j] |= grown[j];
      }
    }

    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    int[] o = new int[w * h];
    for (int i = 0; i < o.length; i++) {
      int r = (px[i] >> 16) & 0xFF;
      int g = (px[i] >> 8) & 0xFF;
      int b = px[i] & 0xFF;
      if (!reg[i]) {
        o[i] = 0xFF000000 | px[i];
        continue;
      }
      if (greySat != null && sat[i] < greySat) {
        continue;
      }
      double a = Math.max(255 - r, Math.max(255 - g, 255 - b)) / 255.0;
      if (a < alphaMin) {
        continue;
      }
      // 光晕"去白"：按不透明度反推原色，再把最亮的通道拉满，保持明亮
      a = Math.min(1.0, a * 1.3);
      double[] c = new double[3];
      double m = 0;
      int[] rgb = {r, g, b};
      for (int k = 0; k < 3; k++) {
        c[k] = Math.max(0, Math.min(255, (rgb[k] - 255 * (1 - a)) / a));
        m = Math.max(m, c[k]);
      }
      if (m == 0) {
        m = 1;
      }
      int argb = (int) Math.rint(a * 255) << 24;
      for (int k = 0; k < 3; k++) {
        argb |= (int) Math.rint(c[k] * 255 / m) << (16 - 8 * k);
      }
      o[i] = argb;
    }
    out.setRGB(0, 0, w, h, o, 0, w);
    return out;
  }

  interface Region {
    boolean test(int i);
  }

  /** 从种子出发，沿上下左右连通的、满足条件的像素扩展出的区域。 */
  private static boolean[] flood(int w, int h, int[] seeds, int seedCount, Region ok) {
    boolean[] reg = new boolean[w * h];
    boolean[] seen = new boolean[w * h];
    int[] stack = new int[w * h]; // 入栈时就标记，每个像素最多入栈一次
    int top = 0;
    for (int s = 0; s < seedCount; s++) {
      top = push(stack, top, seen, seeds[s]);
    }
    while (top > 0) {
      int i = stack[--top];
      if (!ok.test(i)) {
        continue;
      }
      reg[i] = true;
      int x = i % w;
      int y = i / w;
      if (x + 1 < w) {
        top = push(stack, top, seen, i + 1);
      }
      if (x > 0) {
        top = push(stack, top, seen, i - 1);
      }
      if (y + 1 < h) {
        top = push(stack, top, seen, i + w);
      }
      if (y > 0) {
        top = push(stack, top, seen, i - w);
      }
    }
    return reg;
  }

  private static int push(int[] stack, int top, boolean[] seen, int i) {
    if (seen[i]) {
      return top;
    }
    seen[i] = true;
    stack[top] = i;
    return top + 1;
  }

  private static void addChannels(int[][] edge, int index, int rgb) {
    edge[0][index] = (rgb >> 16) & 0xFF;
    edge[1][index] = (rgb >> 8) & 0xFF;
    edge[2][index] = rgb & 0xFF;
  }
}
