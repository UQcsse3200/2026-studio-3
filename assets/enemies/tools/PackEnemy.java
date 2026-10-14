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
 * 把 frames_dir 里 &lt;enemy&gt;_&lt;动画名&gt;_&lt;序号&gt;.png 的帧拼成图集并写出 .atlas。
 *
 * <p>用法: java PackEnemy.java &lt;enemy_id&gt; &lt;frames_dir&gt; &lt;out_dir&gt; [--nearest]
 *
 * <p>--nearest: 像素风图集用 Nearest 滤镜，游戏里缩放时像素块保持锐利
 *
 * <p>idle_0、idle_1、attack_0、hurt_0、death_0 必须有，并且总在前五格（位置和以前一样，default 区域就是 idle_0，图鉴不受影响）；
 * 其余帧（defend、cast、各形态的帧）按 ORDER 的顺序往后排。图集宽 2048，每行 4 帧，高度按行数取 1024 或 2048。
 */
public class PackEnemy {
  static final int S = 512;
  static final int COLUMNS = 4;
  static final List<String> REQUIRED = List.of("idle_0", "idle_1", "attack_0", "hurt_0", "death_0");
  // 其余动画的排列顺序；不在这里的动画排在最后，按名字排序
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

    // 帧之间互不重叠，贴到全透明的底图上就是原样复制
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

  /** 找出这只怪的所有帧：必需的五帧在前，其余按 ORDER 和序号排序。 */
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

  /** 一帧：文件、区域名、序号；在图集里的位置由它排第几决定。 */
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
