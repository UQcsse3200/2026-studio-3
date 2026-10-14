import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 把一张已经去好背景的透明图缩放后放进 512x512 的帧里，和现有的帧对齐。
 *
 * <p>用法: java PlaceFrame.java &lt;in.png&gt; &lt;out.png&gt; &lt;height&gt; &lt;x&gt; &lt;y&gt; [--center]
 *
 * <ul>
 *   <li>height：内容（不透明部分）缩放后的高度，单位是帧上的像素
 *   <li>x, y：默认是内容底边中点放到的位置（站在地上的怪，y 一般是地面线 478）；加 --center 时是内容中心的位置（悬浮的怪）
 * </ul>
 */
public class PlaceFrame {
  static final int FRAME = 512;
  static final int ALPHA_MIN = 16; // 低于这个透明度的淡光晕不算进内容范围

  public static void main(String[] args) throws IOException {
    BufferedImage in = ImageIO.read(new File(args[0]));
    if (in == null) {
      throw new IOException("unsupported image format: " + args[0]);
    }
    int height = Integer.parseInt(args[2]);
    double x = Double.parseDouble(args[3]);
    double y = Double.parseDouble(args[4]);
    boolean center = args.length > 5 && args[5].equals("--center");

    int[] box = contentBox(in);
    double scale = (double) height / (box[3] - box[1]);
    double contentW = (box[2] - box[0]) * scale;
    double left = x - contentW / 2;
    double top = center ? y - height / 2.0 : y - height;

    // 整张图按同一比例缩放，再把内容框的左上角挪到目标位置
    BufferedImage out = new BufferedImage(FRAME, FRAME, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.translate(left - box[0] * scale, top - box[1] * scale);
    g.scale(scale, scale);
    g.drawImage(in, 0, 0, null);
    g.dispose();
    ImageIO.write(out, "png", new File(args[1]));

    int[] placed = contentBox(out);
    System.out.printf(
        "placed %s: scale %.2f, content (%d,%d)-(%d,%d)%n",
        new File(args[1]).getName(), scale, placed[0], placed[1], placed[2], placed[3]);
    if (placed[0] <= 0 || placed[1] <= 0 || placed[2] >= FRAME || placed[3] >= FRAME) {
      System.out.println("  warning: content touches the frame edge and may be cut off");
    }
  }

  /** 不透明内容的包围盒 {left, top, right, bottom}，右下不含。 */
  static int[] contentBox(BufferedImage img) {
    int x0 = img.getWidth();
    int y0 = img.getHeight();
    int x1 = 0;
    int y1 = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (img.getRGB(x, y) >>> 24 >= ALPHA_MIN) {
          x0 = Math.min(x0, x);
          y0 = Math.min(y0, y);
          x1 = Math.max(x1, x + 1);
          y1 = Math.max(y1, y + 1);
        }
      }
    }
    return new int[] {x0, y0, x1, y1};
  }
}
