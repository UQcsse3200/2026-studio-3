package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

class EffectVisualStyleTest {

  @Test
  void storesAllFieldsExactlyAsGiven() {
    EffectVisualStyle style =
        new EffectVisualStyle("images/cards/strike.png", Color.RED, 0.5f, 0.4f, 1.2f, 0.3f);

    assertEquals("images/cards/strike.png", style.iconPath());
    assertEquals(Color.RED, style.color());
    assertEquals(0.5f, style.duration());
    assertEquals(0.4f, style.startScale());
    assertEquals(1.2f, style.endScale());
    assertEquals(0.3f, style.rise());
  }

  @Test
  void iconPathCanBeNullForAGeneratedGlow() {
    EffectVisualStyle style = new EffectVisualStyle(null, Color.WHITE, 0.4f, 0.4f, 1.2f, 0f);

    assertEquals(null, style.iconPath());
  }

  @Test
  void rejectsNullColor() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EffectVisualStyle("path.png", null, 0.5f, 0.4f, 1.2f, 0f));
  }

  @Test
  void rejectsNonPositiveDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EffectVisualStyle("path.png", Color.WHITE, 0f, 0.4f, 1.2f, 0f));
  }

  @Test
  void rejectsNonPositiveScales() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EffectVisualStyle("path.png", Color.WHITE, 0.5f, 0f, 1.2f, 0f));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EffectVisualStyle("path.png", Color.WHITE, 0.5f, 0.4f, 0f, 0f));
  }
}
