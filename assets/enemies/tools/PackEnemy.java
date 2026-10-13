import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * 把 &lt;enemy&gt;_{idle_0,idle_1,attack_0,hurt_0,death_0}.png 拼成 2048x1024 图集并写出 .atlas。
 *
 * <p>用法: java PackEnemy.java &lt;enemy_id&gt; &lt;frames_dir&gt; &lt;out_dir&gt; [--nearest]
 *
 * <p>--nearest: 像素风图集用 Nearest 滤镜，游戏里缩放时像素块保持锐利
 */
public class PackEnemy {
  static final int S = 512;
  // {区域名, 序号, 帧文件后缀, 列, 行}
  static final Object[][] LAYOUT = {
    {"idle", 0, "idle_0", 0, 0},
    {"idle", 1, "idle_1", 1, 0},
    {"attack", 0, "attack_0", 2, 0},
    {"hurt", 0, "hurt_0", 3, 0},
    {"death", 0, "death_0", 0, 1},
  };

  public static void main(String[] args) throws IOException {
    String enemy = args[0];
    File framesDir = new File(args[1]);
    File outDir = new File(args[2]);
    boolean nearest = args.length > 3 && args[3].equals("--nearest");

    // 帧之间互不重叠，贴到全透明的底图上就是原样复制
    BufferedImage sheet = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_ARGB);
    int[] px = new int[S * S];
    for (Object[] row : LAYOUT) {
      File file = new File(framesDir, enemy + "_" + row[2] + ".png");
      BufferedImage frame = ImageIO.read(file);
      if (frame == null || frame.getWidth() != S || frame.getHeight() != S) {
        throw new IllegalStateException(row[2] + " is not a " + S + "x" + S + " PNG: " + file);
      }
      frame.getRGB(0, 0, S, S, px, 0, S);
      for (int i = 0; i < px.length; i++) {
        if (px[i] >>> 24 == 0) {
          px[i] = 0;
        }
      }
      sheet.setRGB((int) row[3] * S, (int) row[4] * S, S, S, px, 0, S);
    }
    ImageIO.write(sheet, "png", new File(outDir, enemy + ".png"));

    String filter = nearest ? "Nearest,Nearest" : "Linear,Linear";
    StringBuilder text = new StringBuilder();
    text.append(enemy).append(".png\nsize: 2048, 1024\nformat: RGBA8888\n");
    text.append("filter: ").append(filter).append("\nrepeat: none\n");
    text.append(region("default", 0, 0, -1));
    for (Object[] row : LAYOUT) {
      text.append(region((String) row[0], (int) row[3] * S, (int) row[4] * S, (int) row[1]));
    }
    Files.write(new File(outDir, enemy + ".atlas").toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    System.out.println("packed " + enemy);
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
}
