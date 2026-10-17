import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Pixelates 512x512 enemy frames: downscale -> shared palette -> hard-edged alpha -> inner outline
 * -> nearest-neighbour upscale.
 *
 * <p>With --flatten, instead "flattens" already pixelated frames into a flatter 2D style: merges
 * small noisy colours into large blocks, reduces colour levels and redraws the outline.
 *
 * <p>Usage: java Pixelate.java [--flatten] [--palette a.png,b.png] &lt;pixel_size&gt; &lt;colors&gt;
 * &lt;out_dir&gt; &lt;frame.png&gt; [&lt;frame.png&gt; ...]
 *
 * <p>Pass all frames of one enemy together so they share one palette. The integer arithmetic for
 * scaling and colour picking matches Pillow, so the output is pixel-identical to the original
 * Python version.
 *
 * <p>--palette: for adding new frames to an existing enemy. Instead of computing a new palette, it
 * uses the colours found in these already pixelated frames, so new frames match the existing ones
 * and the existing frames need not be redone (colors has no effect in this mode).
 *
 * <p>--soft-alpha: keeps semi-transparency (shields, glows and similar effects) by splitting alpha
 * into a few levels instead of only fully transparent or fully opaque; the inner outline is drawn
 * only around the opaque body.
 */
public class Pixelate {
  static final int FRAME = 512;
  static final int ALPHA_CUT = 110; // Pixels below this alpha are removed
  static final double OUTLINE_DARKEN = 0.35; // Inner outline: pixels inside the edge are darkened to this share of their brightness
  static final double ACCENT_SHARE = 0.15; // Keep the bright/vivid colours if their share of a cell exceeds this
  static final int GLOW_MIN_ALPHA = 60; // Semi-transparent glow pixels need at least this alpha
  static final int VIVID_MIN = 2; // Keep the vivid colour if a cell has at least this many vivid pixels
  static final int SMOOTH_PASSES = 2; // --flatten: mode filter passes; more means bigger, flatter blocks
  static final int[] SOFT_LEVELS = {24, 96, 176}; // --soft-alpha: boundaries between alpha levels
  static final int[] SOFT_ALPHAS = {0, 80, 150, 255}; // --soft-alpha: alpha used for each level

  static boolean softAlpha; // --soft-alpha switch, set once per run

  public static void main(String[] args) throws IOException {
    boolean flatten = false;
    List<int[]> fixedPalette = null;
    int first = 0;
    while (args[first].startsWith("--")) {
      if (args[first].equals("--flatten")) {
        flatten = true;
        first++;
      } else if (args[first].equals("--soft-alpha")) {
        softAlpha = true;
        first++;
      } else if (args[first].equals("--palette")) {
        fixedPalette = paletteOf(args[first + 1].split(","));
        first += 2;
      } else {
        throw new IllegalArgumentException("unknown option " + args[first]);
      }
    }
    int size = Integer.parseInt(args[first]);
    int colors = Integer.parseInt(args[first + 1]);
    File outDir = new File(args[first + 2]);
    outDir.mkdirs();
    List<File> paths = new ArrayList<>();
    List<Img> frames = new ArrayList<>();
    for (int i = first + 3; i < args.length; i++) {
      paths.add(new File(args[i]));
      frames.add(Img.read(paths.get(paths.size() - 1)));
    }
    List<Img> results =
        flatten ? flatten(frames, size, colors) : pixelate(frames, size, colors, fixedPalette);
    for (int i = 0; i < paths.size(); i++) {
      results.get(i).write(new File(outDir, paths.get(i).getName()));
      System.out.println((flatten ? "flattened " : "pixelated ") + paths.get(i).getName());
    }
  }

  static List<Img> pixelate(List<Img> frames, int size, int colors, List<int[]> fixedPalette) {
    List<Img> smalls = new ArrayList<>();
    for (Img f : frames) {
      smalls.add(saturate(shrink(f, size), 1.2f));
    }
    List<int[]> palette =
        fixedPalette != null ? fixedPalette : sharedPalette(smalls, colors, 6, 3);
    List<Img> results = new ArrayList<>();
    for (Img small : smalls) {
      Img img = innerOutline(applyPalette(small, palette));
      Img big = img.resizeNearest(img.w * size, img.h * size);
      Img canvas = new Img(FRAME, FRAME);
      int off = Math.max(0, Math.floorDiv(FRAME - big.w, 2));
      canvas.alphaComposite(big.crop(0, 0, Math.min(big.w, FRAME), Math.min(big.h, FRAME)), off, off);
      results.add(canvas);
    }
    return results;
  }

  static List<Img> flatten(List<Img> frames, int size, int colors) {
    List<Img> flat = new ArrayList<>();
    for (Img f : frames) {
      flat.add(flattenGrid(f.resizeNearest(FRAME / size, FRAME / size), SMOOTH_PASSES));
    }
    List<int[]> palette = sharedPalette(flat, colors, 4, 2);
    List<Img> results = new ArrayList<>();
    for (Img g : flat) {
      results.add(innerOutline(applyPalette(g, palette)).resizeNearest(FRAME, FRAME));
    }
    return results;
  }

  static Img flattenGrid(Img grid, int passes) {
    int[][] bands = {grid.channel(16), grid.channel(8), grid.channel(0)};
    for (int pass = 0; pass < passes; pass++) {
      // Each cell takes the most common colour in its 3x3 neighbourhood, so fine gradients and noise
      // merge into larger blocks (per channel, like Pillow)
      for (int c = 0; c < 3; c++) {
        bands[c] = modeFilter(bands[c], grid.w, grid.h);
      }
    }
    Img out = new Img(grid.w, grid.h);
    for (int i = 0; i < out.p.length; i++) {
      out.p[i] = (grid.p[i] & 0xFF000000) | bands[0][i] << 16 | bands[1][i] << 8 | bands[2][i];
    }
    return out;
  }

  /** 3x3 mode filter: values appearing at most twice do not count, and the original is kept. */
  static int[] modeFilter(int[] band, int w, int h) {
    int[] out = new int[band.length];
    int[] hist = new int[256];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        Arrays.fill(hist, 0);
        for (int yy = y - 1; yy <= y + 1; yy++) {
          for (int xx = x - 1; xx <= x + 1; xx++) {
            if (yy >= 0 && yy < h && xx >= 0 && xx < w) {
              hist[band[yy * w + xx]]++;
            }
          }
        }
        int best = 0;
        for (int v = 1; v < 256; v++) {
          if (hist[v] > hist[best]) {
            best = v;
          }
        }
        out[y * w + x] = hist[best] > 2 ? best : band[y * w + x];
      }
    }
    return out;
  }

  static Img shrink(Img frame, int size) {
    int n = (int) Math.rint(FRAME / (double) size);
    // Premultiply by alpha before averaging, so colour from transparent areas does not bleed into edges
    int len = frame.p.length;
    int[] pr = new int[len];
    int[] pg = new int[len];
    int[] pb = new int[len];
    int[] pa = new int[len];
    for (int i = 0; i < len; i++) {
      int c = frame.p[i];
      int a = c >>> 24;
      pr[i] = div255(((c >> 16) & 0xFF) * a);
      pg[i] = div255(((c >> 8) & 0xFF) * a);
      pb[i] = div255((c & 0xFF) * a);
      pa[i] = a;
    }
    int[] cr = boxResize(pr, frame.w, frame.h, n, n);
    int[] cg = boxResize(pg, frame.w, frame.h, n, n);
    int[] cb = boxResize(pb, frame.w, frame.h, n, n);
    int[] ca = boxResize(pa, frame.w, frame.h, n, n);

    Img small = new Img(n, n);
    for (int y = 0; y < n; y++) {
      for (int x = 0; x < n; x++) {
        int a = ca[y * n + x];
        // When a cell has enough glowing/vivid pixels, use their average colour: small highlights such
        // as eyes and orbs are not wiped out by the darker colours around them,
        // and semi-transparent effects (beams, slashes) become solid bright pixels instead of being
        // cut off by the alpha threshold
        List<int[]> accents = new ArrayList<>();
        for (int sy = y * size; sy < Math.min((y + 1) * size, FRAME); sy++) {
          for (int sx = x * size; sx < Math.min((x + 1) * size, FRAME); sx++) {
            int c = frame.get(sx, sy);
            if (isGlowPixel(c)) {
              accents.add(rgb(c));
            }
          }
        }
        List<int[]> vivid = new ArrayList<>();
        for (int[] c : accents) {
          if (isVivid(c)) {
            vivid.add(c);
          }
        }
        // Keep vivid pixels as long as there are a few, averaging only the vivid ones so white
        // highlights do not wash them out (eyes are often only two or three pixels)
        if (vivid.size() >= VIVID_MIN || accents.size() >= size * size * ACCENT_SHARE) {
          List<int[]> keep = new ArrayList<>(vivid.size() >= VIVID_MIN ? vivid : accents);
          for (int[] c : accents) {
            if (isHotCore(c)) {
              keep.add(c);
            }
          }
          // Take only the brightest half, so the bright core of a small light (an eye) is not
          // dimmed by the darker glow around it
          keep.sort((p, q) -> Integer.compare(max(q), max(p)));
          keep = keep.subList(0, Math.max(1, keep.size() / 2));
          // Semi-transparent glows stay semi-transparent under --soft-alpha; highlights on the body
          // such as eyes stay opaque
          int glowAlpha = softAlpha ? Math.max(softLevel(a), SOFT_ALPHAS[1]) : 255;
          small.set(x, y, glowAlpha << 24 | average(keep));
          continue;
        }
        int level = softAlpha ? softLevel(a) : a < ALPHA_CUT ? 0 : 255;
        if (level == 0 || a == 0) {
          small.set(x, y, 0);
          continue;
        }
        double k = 255.0 / a;
        int r = Math.min(255, (int) (cr[y * n + x] * k));
        int g = Math.min(255, (int) (cg[y * n + x] * k));
        int b = Math.min(255, (int) (cb[y * n + x] * k));
        small.set(x, y, level << 24 | r << 16 | g << 8 | b);
      }
    }
    return small;
  }

  /** --soft-alpha: snaps a cell's average alpha to one of the levels. */
  static int softLevel(int a) {
    for (int i = 0; i < SOFT_LEVELS.length; i++) {
      if (a < SOFT_LEVELS[i]) {
        return SOFT_ALPHAS[i];
      }
    }
    return 255;
  }

  /**
   * One palette shared by all frames. Vivid colours (eyes, orbs) and white highlights each get
   * their own reserved slots, so they neither crowd each other out nor get swallowed by the large
   * body colours.
   */
  static List<int[]> sharedPalette(List<Img> smalls, int colors, int vividSlots, int glowSlots) {
    List<int[]> pixels = new ArrayList<>();
    for (Img s : smalls) {
      for (int c : s.p) {
        if (c >>> 24 == 255) {
          pixels.add(rgb(c));
        }
      }
    }
    List<int[]> vivid = new ArrayList<>();
    List<int[]> glow = new ArrayList<>();
    List<int[]> base = new ArrayList<>();
    for (int[] p : pixels) {
      if (isVivid(p)) {
        vivid.add(p);
      } else if (isAccent(p)) {
        glow.add(p);
      }
      if (!isAccent(p)) {
        base.add(p);
      }
    }
    if (base.isEmpty()) {
      base = pixels;
    }
    int reserved = (vivid.isEmpty() ? 0 : vividSlots) + (glow.isEmpty() ? 0 : glowSlots);
    List<int[]> entries = new ArrayList<>(medianCut(base, colors - reserved));
    if (!vivid.isEmpty()) {
      entries.addAll(vividEntries(vivid, vividSlots));
    }
    if (!glow.isEmpty()) {
      entries.addAll(medianCut(glow, glowSlots));
    }
    return entries;
  }

  /** Every opaque colour found in the already pixelated frames, in order of first appearance. */
  static List<int[]> paletteOf(String[] paths) throws IOException {
    Map<Integer, int[]> colors = new LinkedHashMap<>();
    for (String path : paths) {
      for (int c : Img.read(new File(path)).p) {
        if (c >>> 24 == 255) {
          colors.putIfAbsent(c & 0xFFFFFF, rgb(c));
        }
      }
    }
    return new ArrayList<>(colors.values());
  }

  /**
   * Each hue (green, orange, red, cyan...) is guaranteed at least one slot and the rest are shared
   * by area, so small but distinctly coloured details (such as green eyes) are not swallowed by
   * large areas of similar colour.
   */
  static List<int[]> vividEntries(List<int[]> vivid, int slots) {
    int hueBins = 12;
    int minPixels = 3;
    Map<Integer, List<int[]>> bins = new LinkedHashMap<>();
    for (int[] p : vivid) {
      int bin = Math.floorMod((int) (hue(p) * hueBins), hueBins);
      bins.computeIfAbsent(bin, k -> new ArrayList<>()).add(p);
    }
    List<List<int[]>> groups = new ArrayList<>();
    for (List<int[]> g : bins.values()) {
      if (g.size() >= minPixels) {
        groups.add(g);
      }
    }
    groups.sort((p, q) -> Integer.compare(q.size(), p.size()));
    List<int[]> entries = new ArrayList<>();
    for (int i = 0; i < Math.min(slots, groups.size()); i++) {
      entries.add(rgb(average(groups.get(i))));
    }
    if (entries.size() < slots) {
      entries.addAll(medianCut(vivid, slots - entries.size()));
    }
    return entries;
  }

  static boolean isVivid(int[] c) {
    // Vivid colours: red eyes, amber orbs, cyan light
    int hi = max(c);
    int lo = Math.min(c[0], Math.min(c[1], c[2]));
    return hi > 140 && (hi - lo) / (double) hi > 0.55;
  }

  static boolean isHotCore(int[] c) {
    // Glowing cores: very bright and clearly coloured (such as the pale yellow-white centre of an
    // eye), not grey-white
    int hi = max(c);
    int lo = Math.min(c[0], Math.min(c[1], c[2]));
    return hi > 230 && hi - lo > 60;
  }

  static boolean isAccent(int[] c) {
    // Vivid colours, glowing cores, or near-white highlights
    return isVivid(c) || isHotCore(c) || Math.min(c[0], Math.min(c[1], c[2])) > 200;
  }

  static boolean isGlowPixel(int argb) {
    int a = argb >>> 24;
    if (a < GLOW_MIN_ALPHA) {
      return false;
    }
    int[] c = rgb(argb);
    if (isAccent(c)) {
      return true;
    }
    // Semi-transparent bright pixels: the glowing parts of beams, slashes and trails
    return a < 255 && max(c) > 200;
  }

  static Img saturate(Img img, float amount) {
    Img out = new Img(img.w, img.h);
    for (int i = 0; i < img.p.length; i++) {
      int c = img.p[i];
      int[] v = rgb(c);
      int gray = luma(v);
      int argb = c & 0xFF000000;
      for (int ch = 0; ch < 3; ch++) {
        // Extrapolate between the greyscale and original images (in float like ImageEnhance.Color,
        // then truncate)
        float t = gray + amount * (v[ch] - gray);
        int q = t <= 0f ? 0 : t >= 255f ? 255 : (int) t;
        argb |= q << (16 - 8 * ch);
      }
      out.p[i] = argb;
    }
    return out;
  }

  static Img applyPalette(Img small, List<int[]> palette) {
    Img out = new Img(small.w, small.h);
    for (int i = 0; i < small.p.length; i++) {
      int c = small.p[i];
      out.p[i] = (c & 0xFF000000) | nearest(palette, rgb(c));
    }
    return out;
  }

  static Img innerOutline(Img img) {
    Img out = img.copy();
    for (int y = 0; y < img.h; y++) {
      for (int x = 0; x < img.w; x++) {
        int c = img.get(x, y);
        int[] v = rgb(c);
        // Transparent pixels are not outlined; neither are bright glowing pixels, or thin light
        // effects would be darkened along their whole length
        // Under --soft-alpha only the opaque body is outlined; pixels next to a semi-transparent
        // shield also count as the edge
        int a = c >>> 24;
        if (a == 0 || (softAlpha && a < 255) || isAccent(v) || max(v) > 200) {
          continue;
        }
        boolean edge = false;
        int[][] around = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
        for (int[] n : around) {
          int na = img.inside(n[0], n[1]) ? img.get(n[0], n[1]) >>> 24 : 0;
          edge |= softAlpha ? na < 255 : na == 0;
        }
        if (edge) {
          out.set(
              x,
              y,
              0xFF000000
                  | (int) (v[0] * OUTLINE_DARKEN) << 16
                  | (int) (v[1] * OUTLINE_DARKEN) << 8
                  | (int) (v[2] * OUTLINE_DARKEN));
        }
      }
    }
    return out;
  }

  // ---------- Median-cut palette (same as Pillow's MEDIANCUT: always split the box with the most
  // pixels, at the median of its longest luminance-weighted axis) ----------

  /** Returns at most colors colours. */
  static List<int[]> medianCut(List<int[]> pixels, int colors) {
    Map<Integer, int[]> counts = new HashMap<>();
    for (int[] p : pixels) {
      counts.computeIfAbsent(p[0] << 16 | p[1] << 8 | p[2], k -> new int[] {p[0], p[1], p[2], 0})[3]++;
    }
    Box root = new Box(new ArrayList<>(counts.values()), pixels.size());
    BoxHeap heap = new BoxHeap();
    heap.add(root);
    for (int left = colors - 1; left > 0; left--) {
      Box node;
      do {
        node = heap.remove();
      } while (node != null && node.volume() == 1);
      if (node == null) {
        break;
      }
      node.split();
      heap.add(node.left);
      heap.add(node.right);
    }
    List<int[]> entries = new ArrayList<>();
    root.collect(entries);
    return entries.subList(0, Math.min(colors, counts.size()));
  }

  static final class Box {
    final List<int[]> colors; // {r, g, b, count}
    final int pixelCount;
    Box left;
    Box right;

    Box(List<int[]> colors, int pixelCount) {
      this.colors = colors;
      this.pixelCount = pixelCount;
    }

    int range(int axis) {
      int lo = 255;
      int hi = 0;
      for (int[] c : colors) {
        lo = Math.min(lo, c[axis]);
        hi = Math.max(hi, c[axis]);
      }
      return hi - lo;
    }

    int volume() {
      return (range(0) + 1) * (range(1) + 1) * (range(2) + 1);
    }

    void split() {
      int[] weighted = {range(0) * 77, range(1) * 150, range(2) * 29};
      int axis = 0;
      for (int i = 1; i < 3; i++) {
        if (weighted[i] > weighted[axis]) {
          axis = i;
        }
      }
      final int ax = axis;
      List<int[]> sorted = new ArrayList<>(colors);
      sorted.sort((p, q) -> Integer.compare(q[ax], p[ax])); // Largest first
      // Accumulate from the large end and stop past half; colours equal to the stopping value also
      // go to the left
      int cut = 0;
      int total = 0;
      while (cut < sorted.size()) {
        total += sorted.get(cut)[3];
        cut++;
        if (total * 2L > pixelCount) {
          break;
        }
      }
      if (cut < sorted.size()) {
        int value = sorted.get(cut - 1)[ax];
        while (cut < sorted.size() && sorted.get(cut)[ax] == value) {
          cut++;
        }
      }
      if (cut == sorted.size()) {
        // Right side is empty: move the colours equal to the smallest value to the right
        int value = sorted.get(sorted.size() - 1)[ax];
        while (cut > 0 && sorted.get(cut - 1)[ax] == value) {
          cut--;
        }
      }
      left = new Box(new ArrayList<>(sorted.subList(0, cut)), count(sorted.subList(0, cut)));
      right =
          new Box(
              new ArrayList<>(sorted.subList(cut, sorted.size())),
              count(sorted.subList(cut, sorted.size())));
    }

    void collect(List<int[]> out) {
      if (left != null) {
        left.collect(out);
        right.collect(out);
        return;
      }
      long[] sum = new long[3];
      long n = 0;
      for (int[] c : colors) {
        for (int i = 0; i < 3; i++) {
          sum[i] += (long) c[i] * c[3];
        }
        n += c[3];
      }
      out.add(
          new int[] {
            (int) (0.5 + (double) sum[0] / n),
            (int) (0.5 + (double) sum[1] / n),
            (int) (0.5 + (double) sum[2] / n)
          });
    }

    private static int count(List<int[]> colors) {
      int n = 0;
      for (int[] c : colors) {
        n += c[3];
      }
      return n;
    }
  }

  /**
   * A binary max-heap like Pillow's QuantHeap; boxes with equal pixel counts also come out in the
   * same order.
   */
  static final class BoxHeap {
    private Box[] heap = new Box[64];
    private int count;

    void add(Box val) {
      if (count == heap.length - 1) {
        heap = Arrays.copyOf(heap, heap.length * 2);
      }
      int k = ++count;
      while (k != 1) {
        if (val.pixelCount - heap[k / 2].pixelCount <= 0) {
          break;
        }
        heap[k] = heap[k / 2];
        k >>= 1;
      }
      heap[k] = val;
    }

    Box remove() {
      if (count == 0) {
        return null;
      }
      Box top = heap[1];
      Box v = heap[count--];
      int k = 1;
      while (k * 2 <= count) {
        int l = k * 2;
        if (l < count && heap[l].pixelCount - heap[l + 1].pixelCount < 0) {
          l++;
        }
        if (v.pixelCount - heap[l].pixelCount > 0) {
          break;
        }
        heap[k] = heap[l];
        k = l;
      }
      heap[k] = v;
      return top;
    }
  }

  // ---------- Small helpers ----------

  /**
   * Same as Pillow when quantising to a palette: nearest colour at 4-level precision, the earlier
   * one on a tie.
   */
  static int nearest(List<int[]> palette, int[] c) {
    int r = c[0] & 0xFC;
    int g = c[1] & 0xFC;
    int b = c[2] & 0xFC;
    int[] best = palette.get(0);
    int bestDist = Integer.MAX_VALUE;
    for (int[] p : palette) {
      int d = (r - p[0]) * (r - p[0]) + (g - p[1]) * (g - p[1]) + (b - p[2]) * (b - p[2]);
      if (d < bestDist) {
        best = p;
        bestDist = d;
      }
    }
    return best[0] << 16 | best[1] << 8 | best[2];
  }

  /**
   * Same fixed-point arithmetic as Pillow's BOX resize: horizontal first, then vertical, rounding
   * to 8 bits at each step.
   */
  static int[] boxResize(int[] src, int w, int h, int nw, int nh) {
    int[][] kx = boxCoeffs(w, nw);
    int[][] ky = boxCoeffs(h, nh);
    int[] tmp = new int[nw * h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < nw; x++) {
        long ss = 1 << 21;
        int[] k = kx[x];
        for (int i = 1; i < k.length; i++) {
          ss += (long) src[y * w + k[0] + i - 1] * k[i];
        }
        tmp[y * nw + x] = clip8(ss);
      }
    }
    int[] out = new int[nw * nh];
    for (int y = 0; y < nh; y++) {
      int[] k = ky[y];
      for (int x = 0; x < nw; x++) {
        long ss = 1 << 21;
        for (int i = 1; i < k.length; i++) {
          ss += (long) tmp[(k[0] + i - 1) * nw + x] * k[i];
        }
        out[y * nw + x] = clip8(ss);
      }
    }
    return out;
  }

  /** For each output position returns {start, weights...}; weights are 22-bit fixed point. */
  private static int[][] boxCoeffs(int inSize, int outSize) {
    double scale = (double) inSize / outSize;
    double filterScale = Math.max(1.0, scale);
    double support = 0.5 * filterScale;
    int[][] out = new int[outSize][];
    for (int xx = 0; xx < outSize; xx++) {
      double center = (xx + 0.5) * scale;
      int xmin = Math.max(0, (int) (center - support + 0.5));
      int xmax = Math.min(inSize, (int) (center + support + 0.5)) - xmin;
      double[] w = new double[xmax];
      double ww = 0;
      for (int x = 0; x < xmax; x++) {
        double t = (x + xmin - center + 0.5) / filterScale;
        w[x] = t > -0.5 && t <= 0.5 ? 1.0 : 0.0;
        ww += w[x];
      }
      out[xx] = new int[xmax + 1];
      out[xx][0] = xmin;
      for (int x = 0; x < xmax; x++) {
        double k = ww != 0 ? w[x] / ww : w[x];
        out[xx][x + 1] = (int) (0.5 + k * (1 << 22));
      }
    }
    return out;
  }

  private static int clip8(long v) {
    if (v >= 1L << 30) {
      return 255;
    }
    return v <= 0 ? 0 : (int) (v >> 22);
  }

  static int div255(int v) {
    int t = v + 128;
    return ((t >> 8) + t) >> 8;
  }

  /** Same integer formula Pillow uses to convert to greyscale. */
  static int luma(int[] c) {
    return (c[0] * 19595 + c[1] * 38470 + c[2] * 7471 + 0x8000) >> 16;
  }

  /** Hue (0 to 1), same as Python colorsys.rgb_to_hsv. */
  static double hue(int[] c) {
    double r = c[0] / 255.0;
    double g = c[1] / 255.0;
    double b = c[2] / 255.0;
    double hi = Math.max(r, Math.max(g, b));
    double lo = Math.min(r, Math.min(g, b));
    if (hi == lo) {
      return 0.0;
    }
    double range = hi - lo;
    double rc = (hi - r) / range;
    double gc = (hi - g) / range;
    double bc = (hi - b) / range;
    double h = r == hi ? bc - gc : g == hi ? 2.0 + rc - bc : 4.0 + gc - rc;
    double m = (h / 6.0) % 1.0;
    return m < 0 ? m + 1.0 : m;
  }

  static int[] rgb(int argb) {
    return new int[] {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
  }

  static int max(int[] c) {
    return Math.max(c[0], Math.max(c[1], c[2]));
  }

  /** Integer average of each channel, returned as 0xRRGGBB. */
  static int average(List<int[]> colors) {
    long r = 0;
    long g = 0;
    long b = 0;
    for (int[] c : colors) {
      r += c[0];
      g += c[1];
      b += c[2];
    }
    int n = colors.size();
    return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
  }

  /**
   * ARGB pixel array: reads and writes PNG, and does nearest-neighbour scaling, cropping and alpha
   * compositing the same way Pillow does.
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

    Img copy() {
      Img out = new Img(w, h);
      System.arraycopy(p, 0, out.p, 0, p.length);
      return out;
    }

    int[] channel(int shift) {
      int[] out = new int[p.length];
      for (int i = 0; i < p.length; i++) {
        out[i] = (p[i] >>> shift) & 0xFF;
      }
      return out;
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
