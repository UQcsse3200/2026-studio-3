package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.extensions.GameExtension;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CampfireUpgradeAssetsTest {
  @Test
  void preloadsAuthoredFramesAndEveryConfiguredArtworkForUpgradeCards() {
    Set<String> paths = Set.of(CampfireScreen.upgradeCardTexturePaths());
    assertTrue(paths.contains(CardWidgetAssets.COMMON_FRAME_TEXTURE));
    assertTrue(paths.contains(CardWidgetAssets.UNCOMMON_FRAME_TEXTURE));
    assertTrue(paths.contains(CardWidgetAssets.RARE_FRAME_TEXTURE));
    for (var config : CardConfigLoader.loadCards()) {
      if (config.texturePath != null && !config.texturePath.isBlank())
        assertTrue(paths.contains(config.texturePath), config.id);
    }
    for (String path : paths) assertTrue(Gdx.files.internal(path).exists(), path);
  }
}
