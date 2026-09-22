package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.components.library.CardLibraryDisplay;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.extensions.GameExtension;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardLibraryScreenTest {
  @Test
  void collectTexturePathsIncludesMenuAndCardArtwork() {
    List<String> texturePaths = Arrays.asList(CardLibraryScreen.collectTexturePaths());

    assertTrue(texturePaths.contains(MainMenuDisplay.BACKGROUND_TEXTURE));
    assertTrue(texturePaths.contains(CardLibraryDisplay.BUTTON_TEXTURE));
    for (CardConfig card : CardConfigLoader.loadCards()) {
      assertTrue(texturePaths.contains(card.texturePath));
    }
  }

  @Test
  void collectTexturePathsDoesNotDuplicateArtworkPaths() {
    List<String> texturePaths = Arrays.asList(CardLibraryScreen.collectTexturePaths());
    Set<String> uniquePaths = new HashSet<>(texturePaths);

    assertEquals(uniquePaths.size(), texturePaths.size());
  }
}
