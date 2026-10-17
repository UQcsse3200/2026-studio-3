import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Packs the &lt;enemy&gt;_&lt;animation&gt;_&lt;index&gt;.png frames in frames_dir into an atlas and
 * writes the .atlas file.
 *
 * <p>Usage: java PackEnemy.java &lt;enemy_id&gt; &lt;frames_dir&gt; &lt;out_dir&gt; [--nearest]
 *
 * <p>--nearest: use the Nearest filter for pixel-art atlases so pixels stay sharp when scaled in
 * game
 *
 * <p>idle_0, idle_1, attack_0, hurt_0 and death_0 are required and always take the first five
 * slots (same positions as before, the default region is idle_0, so the bestiary is unaffected);
 * the other frames (defend, cast, form frames) follow in ORDER. The atlas is 2048 wide with 4
 * frames per row, and 1024 or 2048 high depending on the number of rows.
 */
public class PackEnemy {
  static final int S = 512;
  static final int COLUMNS = 4;
  static final List<String> REQUIRED = List.of("idle_0", "idle_1", "attack_0", "hurt_0", "death_0");
  // Order of the remaining animations; animations not listed here go last, sorted by name
  static final List<String> ORDER =
      List.of(
          "idle", "attack", "hurt", "death", "defend", "cast", "idle_taunt", "hurt_taunt",
          "idle_enraged", "attack_enraged", "hurt_enraged");

  public static void main(String[] args) throws IOException {
    String enemy = args[0];
    File framesDir = new File(args[1]);
    File outDir = new File(args[2]);
    boolean nearest = args.length > 3 && args[3].equals("--nearest");

    List<Frame> frames = collect(enemy, framesDir);
    int rows = (frames.size() + COLUMNS - 1) / COLUMNS;
    if (rows > 4) {
      throw new IllegalStateException(frames.size() + " frames do not fit in a 2048x2048 sheet");
    }
    int height = rows <= 2 ? 1024 : 2048;

    // Frames never overlap, so drawing onto a fully transparent canvas copies them exactly
    BufferedImage sheet = new BufferedImage(COLUMNS * S, height, BufferedImage.TYPE_INT_ARGB);
    int[] px = new int[S * S];
    for (int i = 0; i < frames.size(); i++) {
      Frame f = frames.get(i);
      BufferedImage img = ImageIO.read(f.file);
      if (img == null || img.getWidth() != S || img.getHeight() != S) {
        throw new IllegalStateException(f.file + " is not a " + S + "x" + S + " PNG");
      }
      img.getRGB(0, 0, S, S, px, 0, S);
      for (int j = 0; j < px.length; j++) {
        if (px[j] >>> 24 == 0) {
          px[j] = 0;
        }
      }
      sheet.setRGB(f.x(i), f.y(i), S, S, px, 0, S);
    }
    ImageIO.write(sheet, "png", new File(outDir, enemy + ".png"));

    String filter = nearest ? "Nearest,Nearest" : "Linear,Linear";
    StringBuilder text = new StringBuilder();
    text.append(enemy).append(".png\nsize: 2048, ").append(height).append("\nformat: RGBA8888\n");
    text.append("filter: ").append(filter).append("\nrepeat: none\n");
    text.append(region("default", 0, 0, -1));
    for (int i = 0; i < frames.size(); i++) {
      Frame f = frames.get(i);
      text.append(region(f.name, f.x(i), f.y(i), f.index));
    }
    Files.write(
        new File(outDir, enemy + ".atlas").toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    System.out.println("packed " + enemy + ": " + frames.size() + " frames, 2048x" + height);
  }

  /** Finds all frames of this enemy: the five required frames first, the rest by ORDER and index. */
  static List<Frame> collect(String enemy, File dir) {
    Pattern pattern = Pattern.compile(Pattern.quote(enemy) + "_([a-z_]+)_(\\d+)\\.png");
    List<Frame> extra = new ArrayList<>();
    List<Frame> frames = new ArrayList<>();
    for (String suffix : REQUIRED) {
      File file = new File(dir, enemy + "_" + suffix + ".png");
      if (!file.isFile()) {
        throw new IllegalStateException("missing required frame " + file);
      }
    }
    String[] names = dir.list();
    Arrays.sort(names);
    for (String name : names) {
      Matcher m = pattern.matcher(name);
      if (m.matches()) {
        Frame f = new Frame(new File(dir, name), m.group(1), Integer.parseInt(m.group(2)));
        (REQUIRED.contains(m.group(1) + "_" + m.group(2)) ? frames : extra).add(f);
      }
    }
    frames.sort((a, b) -> REQUIRED.indexOf(a.key()) - REQUIRED.indexOf(b.key()));
    extra.sort(
        (a, b) -> {
          int oa = ORDER.contains(a.name) ? ORDER.indexOf(a.name) : ORDER.size();
          int ob = ORDER.contains(b.name) ? ORDER.indexOf(b.name) : ORDER.size();
          if (oa != ob) {
            return oa - ob;
          }
          return a.name.equals(b.name) ? a.index - b.index : a.name.compareTo(b.name);
        });
    frames.addAll(extra);
    return frames;
  }

  static String region(String name, int x, int y, int index) {
    return name
        + "\n  rotate: false\n  xy: "
        + x
        + ", "
        + y
        + "\n  size: "
        + S
        + ", "
        + S
        + "\n  orig: "
        + S
        + ", "
        + S
        + "\n  offset: 0, 0\n  index: "
        + index
        + "\n";
  }

  /** One frame: file, region name and index; its atlas position is decided by its place in order. */
  static final class Frame {
    final File file;
    final String name;
    final int index;

    Frame(File file, String name, int index) {
      this.file = file;
      this.name = name;
      this.index = index;
    }

    String key() {
      return name + "_" + index;
    }

    int x(int slot) {
      return slot % COLUMNS * S;
    }

    int y(int slot) {
      return slot / COLUMNS * S;
    }
  }
}
