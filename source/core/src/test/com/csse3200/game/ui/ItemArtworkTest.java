package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.rewards.ItemType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ItemArtworkTest {
  @Test
  void libraryUsesFramedArtAndInGameSurfacesUseCutouts() {
    assertEquals("images/ui/lucky-coin.png", ItemArtwork.framedPath(ItemType.LUCKY_COIN));
    assertEquals(
        "images/ui/item-cutouts/blood-oath-sigil.png",
        ItemArtwork.cutoutPath(ItemType.BLOOD_OATH_SIGIL));
  }

  @Test
  void preloadListIncludesEveryItemOnceAfterOtherTextures() {
    String[] cutouts = ItemArtwork.allCutoutPaths();
    assertEquals(ItemType.values().length, cutouts.length);
    assertEquals(cutouts.length, Arrays.stream(cutouts).distinct().count());

    String[] expected = new String[cutouts.length + 1];
    expected[0] = "images/ui/inventory-panel.png";
    System.arraycopy(cutouts, 0, expected, 1, cutouts.length);
    assertArrayEquals(expected, ItemArtwork.withAllCutouts("images/ui/inventory-panel.png"));
  }
}
