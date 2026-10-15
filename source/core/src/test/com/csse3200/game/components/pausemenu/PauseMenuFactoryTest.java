package com.csse3200.game.components.pausemenu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.save.SaveLoadPanel;
import java.util.List;
import org.junit.jupiter.api.Test;

class PauseMenuFactoryTest {
  @Test
  void menuAssetsIncludeSaveLoadButtonFrame() {
    assertTrue(List.of(PauseMenuFactory.menuTexturePaths()).contains(SaveLoadPanel.BUTTON_TEXTURE));
  }
}
