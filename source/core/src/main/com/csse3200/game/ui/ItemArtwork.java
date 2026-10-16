package com.csse3200.game.ui;

import com.csse3200.game.rewards.ItemType;
import java.util.Locale;
import java.util.Objects;

/** Asset paths for the framed library art and transparent in-game item cutouts. */
public final class ItemArtwork {
  private static final String FRAMED_DIRECTORY = "images/ui/";
  private static final String CUTOUT_DIRECTORY = "images/ui/item-cutouts/";

  private ItemArtwork() {}

  public static String framedPath(ItemType item) {
    return FRAMED_DIRECTORY + fileName(item);
  }

  public static String cutoutPath(ItemType item) {
    return CUTOUT_DIRECTORY + fileName(item);
  }

  /** Includes every item automatically when a new {@link ItemType} is added. */
  public static String[] allCutoutPaths() {
    ItemType[] items = ItemType.values();
    String[] paths = new String[items.length];
    for (int i = 0; i < items.length; i++) {
      paths[i] = cutoutPath(items[i]);
    }
    return paths;
  }

  /**
   * Appends the complete item cutout set to a screen's other textures for loading and unloading.
   */
  public static String[] withAllCutouts(String... otherTextures) {
    String[] cutouts = allCutoutPaths();
    String[] paths = new String[otherTextures.length + cutouts.length];
    System.arraycopy(otherTextures, 0, paths, 0, otherTextures.length);
    System.arraycopy(cutouts, 0, paths, otherTextures.length, cutouts.length);
    return paths;
  }

  private static String fileName(ItemType item) {
    return Objects.requireNonNull(item).name().toLowerCase(Locale.ROOT).replace('_', '-') + ".png";
  }
}
