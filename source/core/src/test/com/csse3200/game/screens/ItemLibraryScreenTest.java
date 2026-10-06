package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.library.CardLibraryDisplay;
import com.csse3200.game.components.library.ItemLibraryDisplay;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemLibraryScreenTest {
  @Test
  void collectTexturePathsIncludesMenuAndItemArtwork() {
    List<String> texturePaths = Arrays.asList(ItemLibraryScreen.collectTexturePaths());

    assertTrue(texturePaths.contains(MainMenuDisplay.BACKGROUND_TEXTURE));
    assertTrue(texturePaths.contains(CardLibraryDisplay.BUTTON_TEXTURE));
    for (ItemType item : ItemType.values()) {
      assertTrue(texturePaths.contains(ItemLibraryDisplay.resolveArtworkPath(item)));
    }
  }

  @Test
  void collectTexturePathsDoesNotDuplicateArtworkPaths() {
    List<String> texturePaths = Arrays.asList(ItemLibraryScreen.collectTexturePaths());
    Set<String> uniquePaths = new HashSet<>(texturePaths);

    assertEquals(uniquePaths.size(), texturePaths.size());
  }

  @Test
  void everyItemHasADistinctDisplayName() {
    Set<String> names = new HashSet<>();
    for (ItemType item : ItemType.values()) {
      String name = ItemFormatting.formatItemName(item);
      assertFalse(name.isBlank());
      names.add(name);
    }

    assertEquals(ItemType.values().length, names.size());
  }
}
