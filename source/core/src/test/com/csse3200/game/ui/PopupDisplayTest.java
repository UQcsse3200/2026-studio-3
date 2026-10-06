package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PopupDisplayTest {
  @Test
  void isNotShowingBeforeComponentCreation() {
    assertFalse(new PopupDisplay("Inventory").isShowing());
  }

  @Test
  void acceptsWindowConfigurationBeforeComponentCreation() {
    PopupDisplay popup = new PopupDisplay("Inventory");

    assertDoesNotThrow(
        () -> {
          popup.setBackgroundTexture("images/ui/inventory-panel.png");
          popup.setPadding(14f, 22f, 14f, 22f);
          popup.setDefaultCloseButtonVisible(false);
          popup.setTitleStyle(Color.WHITE, "font");
        });
  }

  @Test
  void lastBackgroundConfigurationCanBeSetBeforeComponentCreation() {
    PopupDisplay popup = new PopupDisplay("Inventory");

    assertDoesNotThrow(
        () -> {
          popup.setBackgroundColour(Color.BLACK);
          popup.setBackgroundTexture("images/ui/inventory-panel.png");
          popup.setBackgroundColour(Color.DARK_GRAY);
        });
  }
}
