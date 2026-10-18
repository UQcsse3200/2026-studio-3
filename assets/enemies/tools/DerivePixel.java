import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Derives idle_1 / hurt_0 / death_0 from a pixelated idle_0. Every change moves whole cells, so the
 * pixel grid is never broken.
 *
 * <p>Usage: java DerivePixel.java &lt;pixel_size&gt; &lt;frames_dir&gt; &lt;enemy_id&gt; [--floating] [--form
 * &lt;name&gt;]
 *
 * <p>--form: derive frames for a form, e.g. --form taunt derives idle_taunt_1 and hurt_taunt_0 from
 * idle_taunt_0 (forms have no death frame).
 */
public class DerivePixel {
  static final int FRAME = 512;
  static final double BREATH_SHARE = 0.7; // Standing enemies: this upper share of the body sinks 1 cell in idle_1
  static final int FLOAT_LIFT = 2; // Floating enemies: cells the whole body rises in idle_1
  static final double DEATH_SQUASH = 0.75; // Death frame is squashed to this share of its height
  static final int[] HURT_TINT = {255, 70, 60};
  static final int GROUND_Y = 478; // Ground line (pixel row on a 512 frame), same as tomb_guardian

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
    // Form frame names carry a suffix, e.g. idle_taunt_0, hurt_taunt_0
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
    out.paste(grid.crop(0, cut, grid.w, grid.h), 0, cut); // Lower body stays, feet on the ground
    out.alphaComposite(grid.crop(0, 0, grid.w, cut), 0, 1); // Upper body sinks 1 cell
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
        int red = gray * HURT_TINT[ch] / 255; // Gradient map from black to hurt red
        argb |= (int) (src + 0.75f * (red - src)) << shift; // Mix original and red 1:3
      }
      tinted.p[i] = argb;
    }
    Img out = new Img(grid.w, grid.h);
    out.paste(tinted, 1, 0); // Lean back 1 cell to the right (away from the player)
    return out;
  }

  static Img death(Img grid, boolean floating, int size) {
    Img gray = new Img(grid.w, grid.h);
    for (int i = 0; i < grid.p.length; i++) {
      int c = grid.p[i];
      int v = (int) (0.8f * luma(c)); // Desaturate and darken to 80%
      int a = (c >>> 24) == 0 ? 0 : 215;
      gray.p[i] = a << 24 | v << 16 | v << 8 | v;
    }
    int[] box = gray.alphaBox();
    Img body = gray.crop(box[0], box[1], box[2], box[3]);
    body = body.resizeNearest(body.w, Math.max(1, (int) Math.rint(body.h * DEATH_SQUASH)));
    // Standing enemies collapse to their original lowest point; floating ones drop to the ground line
    int floor = floating ? (int) Math.rint(GROUND_Y / (double) size) : box[3];
    Img out = new Img(grid.w, grid.h);
    out.paste(body, box[0], floor - body.h);
    return out;
  }

  /** Same integer formula Pillow uses to convert to greyscale. */
  static int luma(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return (r * 19595 + g * 38470 + b * 7471 + 0x8000) >> 16;
  }

  /**
   * ARGB pixel array: reads and writes PNG, and does nearest-neighbour scaling, cropping, pasting
   * and alpha compositing the same way Pillow does.
   */
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

    /** Bounding box of the opaque area {left, top, right, bottom}, right and bottom exclusive. */
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

    /** Overwrites directly (alpha included); anything outside the bounds is dropped. */
    void paste(Img src, int dx, int dy) {
      for (int y = 0; y < src.h; y++) {
        for (int x = 0; x < src.w; x++) {
          if (inside(x + dx, y + dy)) {
            set(x + dx, y + dy, src.get(x, y));
          }
        }
      }
    }

    /** Nearest-neighbour scaling, accumulating coordinates in double the way Pillow does. */
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

    /**
     * Alpha-composites src at (dx, dy), using the same integer arithmetic as Pillow's
     * alpha_composite.
     */
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
