package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleMenuLayoutTest {
  @Test
  void titleAndThreeButtonBoundsStayFixedAcrossWindowSizes() {
    Stage stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    try {
      Texture texture = mock(Texture.class);
      when(texture.getWidth()).thenReturn(1958);
      when(texture.getHeight()).thenReturn(803);
      List<ClickableRecord> buttons =
          ClickableFactory.loadRecordsFromJson(Path.of("sprites/BattleUi.json"));

      assertEquals(3, buttons.size());
      assertButton(buttons.get(0), "Card Inventory", 24f, 180f, 247f, 48f);
      assertButton(buttons.get(1), "Item Inventory", 24f, 239f, 247f, 48f);
      assertButton(buttons.get(2), "End Turn", 24f, 299f, 154f, 48f);

      stage.getViewport().update(1280, 800, true);
      Image firstTitle = BattleScreen.createBattleTitle(stage, texture);
      assertTitleBounds(firstTitle);
      assertEquals(Touchable.disabled, firstTitle.getTouchable());

      stage.getViewport().update(1920, 1080, true);
      Image resizedTitle = BattleScreen.createBattleTitle(stage, texture);
      assertTitleBounds(resizedTitle);
    } finally {
      stage.dispose();
    }
  }

  private static void assertButton(
      ClickableRecord button, String text, float x, float y, float width, float height) {
    assertEquals(text, button.text());
    assertEquals(x, button.x(), 0.01f);
    assertEquals(y, button.y(), 0.01f);
    assertEquals(width, button.width(), 0.01f);
    assertEquals(height, button.height(), 0.01f);
  }

  private static void assertTitleBounds(Image title) {
    assertEquals(36f, title.getX(), 0.01f);
    assertEquals(667f, title.getY(), 0.01f);
    assertEquals(222f, title.getWidth(), 0.01f);
    assertEquals(91f, title.getHeight(), 0.01f);
  }
}
