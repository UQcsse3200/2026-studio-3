import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Recolours the glowing parts of a pixel frame within one hue range, leaving everything else
 * unchanged. Used to make form frames from existing ones, e.g. turning the cyan glow in Riven
 * Knight's armour cracks red (enraged form).
 *
 * <p>Usage: java Recolor.java &lt;hue_min&gt; &lt;hue_max&gt; &lt;min_sat&gt; &lt;min_val&gt; &lt;hue_shift&gt;
 * &lt;sat_scale&gt; &lt;in.png&gt; &lt;out.png&gt; [&lt;in.png&gt; &lt;out.png&gt; ...]
 *
 * <ul>
 *   <li>hue_min / hue_max: the hue range to recolour (0 to 360 degrees)
 *   <li>min_sat / min_val: minimum saturation and brightness to count as glowing (0 to 1), so dark
 *       armour and outlines are left alone
 *   <li>hue_shift: hue rotation in degrees, e.g. 180 turns cyan into red
 *   <li>sat_scale: saturation multiplier; above 1 makes the new colour richer
 * </ul>
 *
 * <p>Cores at or above CORE_VAL brightness are recoloured too even when barely saturated (nearly
 * white), so they pick up a hint of the new colour.
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
