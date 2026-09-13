package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import org.junit.jupiter.api.Test;

class DisplayingRecordTest {

  // --- compact constructor default ---

  @Test
  void compactConstructor_defaultsVariantToDisplayingWhenNull() {
    DisplayingRecord rec =
        new DisplayingRecord("Health: 10", null, null, null, null, 0, 0, -1, -1, 1f, null);

    assertEquals("Displaying", rec.variant());
  }

  @Test
  void compactConstructor_keepsExplicitVariantInsteadOfDefaulting() {
    DisplayingRecord rec =
        new DisplayingRecord("Health: 10", null, null, null, null, 0, 0, -1, -1, 1f, "floating");

    assertEquals("floating", rec.variant());
  }

  // --- hasSize() ---

  @Test
  void hasSize_falseWhenBothDimensionsDefault() {
    DisplayingRecord rec = DisplayingRecord.builder("Health: 10").build();

    assertFalse(rec.hasSize());
  }

  @Test
  void hasSize_trueWhenBothDimensionsSet() {
    DisplayingRecord rec = DisplayingRecord.builder("Health: 10").size(64, 32).build();

    assertTrue(rec.hasSize());
  }

  @Test
  void hasSize_falseWhenOnlyWidthSet() {
    DisplayingRecord rec =
        new DisplayingRecord("Health: 10", null, null, null, null, 0, 0, 64, -1, 1f, null);

    assertFalse(rec.hasSize());
  }

  @Test
  void hasSize_falseWhenOnlyHeightSet() {
    DisplayingRecord rec =
        new DisplayingRecord("Health: 10", null, null, null, null, 0, 0, -1, 32, 1f, null);

    assertFalse(rec.hasSize());
  }

  // --- Builder defaults ---

  @Test
  void builder_appliesDefaultsWhenOnlyTextGiven() {
    DisplayingRecord rec = DisplayingRecord.builder("Health: 10").build();

    assertEquals("Health: 10", rec.text());
    assertNull(rec.trigger());
    assertNull(rec.skin());
    assertNull(rec.fontName());
    assertNull(rec.colour());
    assertEquals(1f, rec.scale()); // NO_SCALE default
    assertEquals("Displaying", rec.variant());
    assertFalse(rec.hasSize());
  }

  @Test
  void builder_setsAllFieldsWhenProvided() {
    Skin skin = mock(Skin.class);

    DisplayingRecord rec =
        DisplayingRecord.builder("Health: 10")
            .trigger("healthChanged")
            .skin(skin)
            .fontName("pixel-32")
            .colour("#FF0000")
            .position(10f, 20f)
            .size(64, 32)
            .scale(1.5f)
            .variant("floating")
            .build();

    assertEquals("Health: 10", rec.text());
    assertEquals("healthChanged", rec.trigger());
    assertEquals(skin, rec.skin());
    assertEquals("pixel-32", rec.fontName());
    assertEquals("#FF0000", rec.colour());
    assertEquals(10f, rec.x());
    assertEquals(20f, rec.y());
    assertTrue(rec.hasSize());
    assertEquals(64f, rec.width());
    assertEquals(32f, rec.height());
    assertEquals(1.5f, rec.scale());
    assertEquals("floating", rec.variant());
  }

  @Test
  void builder_acceptsNonStringCharSequenceForText() {
    CharSequence text = new StringBuilder("Health: ").append(10);

    DisplayingRecord rec = DisplayingRecord.builder(text).build();

    assertEquals("Health: 10", rec.text().toString());
  }
}
