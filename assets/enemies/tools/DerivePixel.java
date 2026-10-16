import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 从像素化后的 idle_0 派生 idle_1 / hurt_0 / death_0，所有变化都按整格进行，不破坏像素颗粒。
 *
 * <p>用法: java DerivePixel.java &lt;pixel_size&gt; &lt;frames_dir&gt; &lt;enemy_id&gt; [--floating] [--form
 * &lt;name&gt;]
 *
 * <p>--form：给某个形态派生，例如 --form taunt 从 idle_taunt_0 派生 idle_taunt_1 和 hurt_taunt_0（形态没有死亡帧）。
 */
public class DerivePixel {
  static final int FRAME = 512;
  static final double BREATH_SHARE = 0.7; // 站立的怪：上面这个比例的身体在 idle_1 里下压 1 格
  static final int FLOAT_LIFT = 2; // 悬浮的怪：idle_1 整体上浮的格数
  static final double DEATH_SQUASH = 0.75; // 死亡帧纵向压扁到原高度的这个比例
  static final int[] HURT_TINT = {255, 70, 60};
  static final int GROUND_Y = 478; // 地面线（512 帧上的像素行），和 tomb_guardian 一致

  public static void main(String[] args) throws IOException {
    int size = Integer.parseInt(args[0]);
    File dir = new File(args[1]);
    String enemy = args[2];
    boolean floating = false;
    String form = null;
    for (int i = 3; i < args.length; i++) {
      if (args[i].equals("--floating")) {
        floating = true;
      } else if (args[i].equals("--form")) {
        form = args[++i];
      } else {
        throw new IllegalArgumentException("unknown option " + args[i]);
      }
    }
    // 形态的帧名带后缀，比如 idle_taunt_0、hurt_taunt_0
    String suffix = form == null ? "" : "_" + form;

    Img grid =
        Img.read(new File(dir, enemy + "_idle" + suffix + "_0.png"))
            .resizeNearest(FRAME / size, FRAME / size);
    String[] names =
        form == null
            ? new String[] {"idle_1", "hurt_0", "death_0"}
            : new String[] {"idle" + suffix + "_1", "hurt" + suffix + "_0"};
    Img[] derived =
        form == null
            ? new Img[] {breathe(grid, floating), hurt(grid), death(grid, floating, size)}
            : new Img[] {breathe(grid, floating), hurt(grid)};
    for (int i = 0; i < names.length; i++) {
      Img frame = derived[i].resizeNearest(derived[i].w * size, derived[i].h * size);
      frame.write(new File(dir, enemy + "_" + names[i] + ".png"));
      System.out.println("derived " + enemy + "_" + names[i] + ".png");
    }
  }

  static Img breathe(Img grid, boolean floating) {
    Img out = new Img(grid.w, grid.h);
    if (floating) {
      out.paste(grid, 0, -FLOAT_LIFT);
      return out;
    }
    int[] box = grid.alphaBox();
    int cut = box[1] + (int) Math.rint((box[3] - box[1]) * BREATH_SHARE);
    out.paste(grid.crop(0, cut, grid.w, grid.h), 0, cut); // 下半身不动，脚不离地
    out.alphaComposite(grid.crop(0, 0, grid.w, cut), 0, 1); // 上半身下压 1 格
    return out;
  }

  static Img hurt(Img grid) {
    Img tinted = new Img(grid.w, grid.h);
    for (int i = 0; i < grid.p.length; i++) {
      int c = grid.p[i];
      int gray = luma(c);
      int argb = c & 0xFF000000;
      for (int ch = 0; ch < 3; ch++) {
        int shift = 16 - 8 * ch;
        int src = (c >> shift) & 0xFF;
        int red = gray * HURT_TINT[ch] / 255; // 从黑到受伤红的渐变映射
        argb |= (int) (src + 0.75f * (red - src)) << shift; // 原色和红色 1:3 混合
      }
      tinted.p[i] = argb;
    }
    Img out = new Img(grid.w, grid.h);
    out.paste(tinted, 1, 0); // 往右（远离玩家）后仰 1 格
    return out;
  }

  static Img death(Img grid, boolean floating, int size) {
    Img gray = new Img(grid.w, grid.h);
    for (int i = 0; i < grid.p.length; i++) {
      int c = grid.p[i];
      int v = (int) (0.8f * luma(c)); // 去色并压暗到 80%
      int a = (c >>> 24) == 0 ? 0 : 215;
      gray.p[i] = a << 24 | v << 16 | v << 8 | v;
    }
    int[] box = gray.alphaBox();
    Img body = gray.crop(box[0], box[1], box[2], box[3]);
    body = body.resizeNearest(body.w, Math.max(1, (int) Math.rint(body.h * DEATH_SQUASH)));
    // 站立的怪塌到原来的最低点；悬浮的怪落到地面线上
    int floor = floating ? (int) Math.rint(GROUND_Y / (double) size) : box[3];
    Img out = new Img(grid.w, grid.h);
    out.paste(body, box[0], floor - body.h);
    return out;
  }

  /** 和 Pillow 转灰度相同的整数公式。 */
  static int luma(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return (r * 19595 + g * 38470 + b * 7471 + 0x8000) >> 16;
  }

  /** ARGB 像素数组，读写 PNG，以及和 Pillow 行为一致的最近邻缩放、裁剪、粘贴、透明叠加。 */
  static final class Img {
    final int w;
    final int h;
    final int[] p;

    Img(int w, int h) {
      this.w = w;
      this.h = h;
      this.p = new int[w * h];
    }

    static Img read(File f) throws IOException {
      BufferedImage bi = ImageIO.read(f);
      if (bi == null) {
        throw new IOException("unsupported image format: " + f);
      }
      Img img = new Img(bi.getWidth(), bi.getHeight());
      bi.getRGB(0, 0, img.w, img.h, img.p, 0, img.w);
      return img;
    }

    void write(File f) throws IOException {
      BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      bi.setRGB(0, 0, w, h, p, 0, w);
      ImageIO.write(bi, "png", f);
    }

    int get(int x, int y) {
      return p[y * w + x];
    }

    void set(int x, int y, int argb) {
      p[y * w + x] = argb;
    }

    boolean inside(int x, int y) {
      return x >= 0 && y >= 0 && x < w && y < h;
    }

    /** 不透明区域的包围盒 {left, top, right, bottom}，右下不含。 */
    int[] alphaBox() {
      int x0 = w;
      int y0 = h;
      int x1 = 0;
      int y1 = 0;
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          if (get(x, y) >>> 24 != 0) {
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x + 1);
            y1 = Math.max(y1, y + 1);
          }
        }
      }
      return new int[] {x0, y0, x1, y1};
    }

    Img crop(int x0, int y0, int x1, int y1) {
      Img out = new Img(x1 - x0, y1 - y0);
      for (int y = y0; y < y1; y++) {
        for (int x = x0; x < x1; x++) {
          if (inside(x, y)) {
            out.set(x - x0, y - y0, get(x, y));
          }
        }
      }
      return out;
    }

    /** 直接覆盖（连透明度一起），超出边界的部分丢掉。 */
    void paste(Img src, int dx, int dy) {
      for (int y = 0; y < src.h; y++) {
        for (int x = 0; x < src.w; x++) {
          if (inside(x + dx, y + dy)) {
            set(x + dx, y + dy, src.get(x, y));
          }
        }
      }
    }

    /** 最近邻缩放，坐标按 Pillow 的方式用 double 累加。 */
    Img resizeNearest(int nw, int nh) {
      Img out = new Img(nw, nh);
      double ax = (double) w / nw;
      double ay = (double) h / nh;
      int[] xin = new int[nw];
      double xo = ax * 0.5;
      for (int x = 0; x < nw; x++) {
        xin[x] = xo < 0 ? -1 : (int) xo;
        xo += ax;
      }
      double yo = ay * 0.5;
      for (int y = 0; y < nh; y++) {
        int yi = yo < 0 ? -1 : (int) yo;
        if (yi >= 0 && yi < h) {
          for (int x = 0; x < nw; x++) {
            if (xin[x] >= 0 && xin[x] < w) {
              out.set(x, y, get(xin[x], yi));
            }
          }
        }
        yo += ay;
      }
      return out;
    }

    /** 把 src 按透明度叠加到 (dx, dy)，整数运算和 Pillow 的 alpha_composite 相同。 */
    void alphaComposite(Img src, int dx, int dy) {
      for (int y = 0; y < src.h; y++) {
        for (int x = 0; x < src.w; x++) {
          if (inside(x + dx, y + dy)) {
            set(x + dx, y + dy, over(src.get(x, y), get(x + dx, y + dy)));
          }
        }
      }
    }

    private static int over(int s, int d) {
      long sa = s >>> 24;
      if (sa == 0) {
        return d;
      }
      long da = d >>> 24;
      long blend = da * (255 - sa);
      long outa255 = sa * 255 + blend;
      long coef1 = sa * 255 * 255 * 128 / outa255;
      long coef2 = 255 * 128 - coef1;
      int out = (int) shiftDiv255(outa255 + 0x80) << 24;
      for (int shift = 16; shift >= 0; shift -= 8) {
        long t = ((s >>> shift) & 0xFF) * coef1 + ((d >>> shift) & 0xFF) * coef2;
        out |= (int) (shiftDiv255(t + (0x80 << 7)) >> 7) << shift;
      }
      return out;
    }

    private static long shiftDiv255(long a) {
      return ((a >> 8) + a) >> 8;
    }
  }
}
