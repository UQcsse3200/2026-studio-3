import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 给像素帧里某一段色相的发光部分换颜色，其余部分不动。用来从原有帧做出形态帧，例如把 Riven Knight 盔甲裂纹的青光改成红光（狂暴形态）。
 *
 * <p>用法: java Recolor.java &lt;hue_min&gt; &lt;hue_max&gt; &lt;min_sat&gt; &lt;min_val&gt; &lt;hue_shift&gt;
 * &lt;sat_scale&gt; &lt;in.png&gt; &lt;out.png&gt; [&lt;in.png&gt; &lt;out.png&gt; ...]
 *
 * <ul>
 *   <li>hue_min / hue_max：要换色的色相范围（0~360 度）
 *   <li>min_sat / min_val：饱和度、亮度至少这么高才算发光部分（0~1），暗的盔甲和描边不会被改到
 *   <li>hue_shift：色相旋转的角度，例如 180 把青色转成红色
 *   <li>sat_scale：饱和度乘的倍数，大于 1 让新颜色更浓
 * </ul>
 *
 * <p>亮度不低于 CORE_VAL 的光芯即使饱和度很低（接近白色）也一起换色，让光芯带一点新颜色。
 */
public class Recolor {
  static final float CORE_VAL = 0.95f;

  public static void main(String[] args) throws IOException {
    float hueMin = Float.parseFloat(args[0]) / 360f;
    float hueMax = Float.parseFloat(args[1]) / 360f;
    float minSat = Float.parseFloat(args[2]);
    float minVal = Float.parseFloat(args[3]);
    float shift = Float.parseFloat(args[4]) / 360f;
    float satScale = Float.parseFloat(args[5]);
    for (int i = 6; i + 1 < args.length; i += 2) {
      BufferedImage img = ImageIO.read(new File(args[i]));
      if (img == null) {
        throw new IOException("unsupported image format: " + args[i]);
      }
      int changed = 0;
      BufferedImage out =
          new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < img.getHeight(); y++) {
        for (int x = 0; x < img.getWidth(); x++) {
          int c = img.getRGB(x, y);
          float[] hsv = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
          boolean glow =
              c >>> 24 != 0
                  && hsv[0] >= hueMin
                  && hsv[0] <= hueMax
                  && hsv[2] >= minVal
                  && (hsv[1] >= minSat || hsv[2] >= CORE_VAL);
          if (glow) {
            float hue = (hsv[0] + shift) % 1f;
            int rgb = Color.HSBtoRGB(hue, Math.min(1f, hsv[1] * satScale), hsv[2]) & 0xFFFFFF;
            c = (c & 0xFF000000) | rgb;
            changed++;
          }
          out.setRGB(x, y, c);
        }
      }
      ImageIO.write(out, "png", new File(args[i + 1]));
      System.out.println("recoloured " + new File(args[i + 1]).getName() + ": " + changed + " px");
    }
  }
}
