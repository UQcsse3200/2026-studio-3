import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 把 128x128 的意图/状态图标像素化：按格取色 -> 每个图标单独的小调色板 -> 统一 1 格白描边 -> 最近邻放大。
 *
 * <p>用法（JDK 11+ 可直接运行源文件）: java PixelateIcon.java &lt;pixel_size&gt; &lt;colors&gt;
 * &lt;out_dir&gt; &lt;icon.png&gt; [&lt;icon.png&gt; ...]
 */
public class PixelateIcon {
  private static final int ALPHA_CUT = 128; // 格子平均不透明度低于这个值就算透明
  private static final double WHITE_SHARE = 0.5; // 格子里白色像素占比达到这个值就画成白色（保留图形之间的白色分隔线）
  private static final int WHITE = 0xFFFFFFFF;
  private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  public static void main(String[] args) throws IOException {
    int size = Integer.parseInt(args[0]);
    int colors = Integer.parseInt(args[1]);
    File outDir = new File(args[2]);
    outDir.mkdirs();
    for (int i = 3; i < args.length; i++) {
      File in = new File(args[i]);
      BufferedImage out = pixelate(ImageIO.read(in), size, colors);
      ImageIO.write(out, "png", new File(outDir, in.getName()));
      System.out.println("pixelated " + in.getName());
    }
  }

  static BufferedImage pixelate(BufferedImage img, int size, int colors) {
    int w = img.getWidth();
    int n = w / size;
    int[][] palette = palette(img, colors);
    boolean[][] solid = new boolean[n][n];
    boolean[][] white = new boolean[n][n];
    int[][] fill = new int[n][n];

    for (int y = 0; y < n; y++) {
      for (int x = 0; x < n; x++) {
        long alphaSum = 0;
        int body = 0;
        int col = 0;
        long r = 0;
        long g = 0;
        long b = 0;
        for (int sy = y * size; sy < (y + 1) * size; sy++) {
          for (int sx = x * size; sx < (x + 1) * size; sx++) {
            int argb = img.getRGB(sx, sy);
            int a = argb >>> 24;
            alphaSum += a;
            if (a < ALPHA_CUT) {
              continue;
            }
            body++;
            if (!isWhite(argb)) {
              col++;
              r += (argb >> 16) & 0xFF;
              g += (argb >> 8) & 0xFF;
              b += argb & 0xFF;
            }
          }
        }
        solid[y][x] = alphaSum / (size * size) >= ALPHA_CUT;
        if (!solid[y][x]) {
          continue;
        }
        white[y][x] = col == 0 || body - col >= WHITE_SHARE * body;
        if (col > 0) {
          // 只平均有色像素，再贴到调色板上：渐变变成整齐的色带，不会被白边冲淡
          fill[y][x] = nearest(palette, (int) (r / col), (int) (g / col), (int) (b / col));
        }
      }
    }

    BufferedImage out = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < n; y++) {
      for (int x = 0; x < n; x++) {
        if (!solid[y][x]) {
          continue;
        }
        // 轮廓上的格子一律白色：外描边粗细统一为 1 格
        boolean edge = false;
        for (int[] d : NEIGHBOURS) {
          edge |= outside(solid, x + d[0], y + d[1]);
        }
        int argb = edge || white[y][x] ? WHITE : fill[y][x];
        for (int sy = y * size; sy < (y + 1) * size; sy++) {
          for (int sx = x * size; sx < (x + 1) * size; sx++) {
            out.setRGB(sx, sy, argb);
          }
        }
      }
    }
    return out;
  }

  private static boolean isWhite(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return Math.min(r, Math.min(g, b)) > 215;
  }

  private static boolean outside(boolean[][] solid, int x, int y) {
    int n = solid.length;
    return x < 0 || y < 0 || x >= n || y >= n || !solid[y][x];
  }

  /** 中位切分：调色板只从有色像素里取，白色描边单独处理，不占名额。 */
  private static int[][] palette(BufferedImage img, int colors) {
    List<int[]> pixels = new ArrayList<>();
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        int argb = img.getRGB(x, y);
        if (argb >>> 24 >= ALPHA_CUT && !isWhite(argb)) {
          pixels.add(new int[] {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF});
        }
      }
    }
    List<List<int[]>> boxes = new ArrayList<>();
    boxes.add(pixels);
    while (boxes.size() < colors) {
      // 每次切开像素最多、且颜色还有差异的那个盒子
      List<int[]> box = null;
      for (List<int[]> candidate : boxes) {
        if (widestChannel(candidate)[1] > 0 && (box == null || candidate.size() > box.size())) {
          box = candidate;
        }
      }
      if (box == null) {
        break;
      }
      int channel = widestChannel(box)[0];
      box.sort(Comparator.comparingInt(p -> p[channel]));
      int mid = box.size() / 2;
      boxes.remove(box);
      boxes.add(new ArrayList<>(box.subList(0, mid)));
      boxes.add(new ArrayList<>(box.subList(mid, box.size())));
    }
    int[][] palette = new int[boxes.size()][3];
    for (int i = 0; i < boxes.size(); i++) {
      List<int[]> box = boxes.get(i);
      for (int[] p : box) {
        for (int c = 0; c < 3; c++) {
          palette[i][c] += p[c];
        }
      }
      for (int c = 0; c < 3; c++) {
        palette[i][c] /= box.size();
      }
    }
    return palette;
  }

  /** 返回 {跨度最大的通道, 该通道的跨度}。 */
  private static int[] widestChannel(List<int[]> box) {
    int best = 0;
    int bestRange = -1;
    for (int c = 0; c < 3; c++) {
      int lo = 255;
      int hi = 0;
      for (int[] p : box) {
        lo = Math.min(lo, p[c]);
        hi = Math.max(hi, p[c]);
      }
      if (hi - lo > bestRange) {
        best = c;
        bestRange = hi - lo;
      }
    }
    return new int[] {best, bestRange};
  }

  private static int nearest(int[][] palette, int r, int g, int b) {
    int[] best = palette[0];
    int bestDist = Integer.MAX_VALUE;
    for (int[] p : palette) {
      int dist = (p[0] - r) * (p[0] - r) + (p[1] - g) * (p[1] - g) + (p[2] - b) * (p[2] - b);
      if (dist < bestDist) {
        best = p;
        bestDist = dist;
      }
    }
    return 0xFF000000 | best[0] << 16 | best[1] << 8 | best[2];
  }
}
