package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord.ButtonType;
import org.junit.jupiter.api.Test;

class ClickableRecordTest {

  // --- compact constructor defaults (only exercised via the canonical constructor directly,
  // since Builder always supplies non-null variant/args) ---

  @Test
  void compactConstructor_defaultsVariantToClickableWhenNull() {
    ClickableRecord rec =
        new ClickableRecord(null, null, 0, 0, null, "attackCard", null, -1, -1, null, null, null);

    assertEquals("Clickable", rec.variant());
  }

  @Test
  void compactConstructor_defaultsArgsToEmptyArrayWhenNull() {
    ClickableRecord rec =
        new ClickableRecord(null, null, 0, 0, null, "attackCard", null, -1, -1, "drag", null, null);

    assertEquals(0, rec.args().length);
  }

  @Test
  void compactConstructor_defaultsLabelToTriggerWhenNull() {
    ClickableRecord rec =
        new ClickableRecord(null, null, 0, 0, null, "attackCard", null, -1, -1, "drag", null, null);

    assertEquals("attackCard", rec.label());
  }

  @Test
  void compactConstructor_keepsExplicitValuesInsteadOfDefaulting() {
    Object[] args = {1, "poison"};
    ClickableRecord rec =
        new ClickableRecord(
            "Attack",
            null,
            0,
            0,
            "default",
            "attackCard",
            null,
            -1,
            -1,
            "drag",
            args,
            "custom label");

    assertEquals("drag", rec.variant());
    assertEquals(args, rec.args());
    assertEquals("custom label", rec.label());
  }

  // --- hasSize() ---

  @Test
  void hasSize_falseWhenBothDimensionsDefault() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").build();

    assertFalse(rec.hasSize());
  }

  @Test
  void hasSize_trueWhenBothDimensionsSet() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").size(64, 32).build();

    assertTrue(rec.hasSize());
  }

  @Test
  void hasSize_falseWhenOnlyWidthSet() {
    ClickableRecord rec =
        new ClickableRecord(
            "Attack", null, 0, 0, null, "attackCard", null, 64, -1, "drag", null, null);

    assertFalse(rec.hasSize());
  }

  @Test
  void hasSize_falseWhenOnlyHeightSet() {
    ClickableRecord rec =
        new ClickableRecord(
            "Attack", null, 0, 0, null, "attackCard", null, -1, 32, "drag", null, null);

    assertFalse(rec.hasSize());
  }

  // --- Builder defaults ---

  @Test
  void builder_appliesDefaultsWhenOnlyTriggerGiven() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").build();

    assertEquals("attackCard", rec.trigger());
    assertNull(rec.text());
    assertNull(rec.btnSkin());
    assertNull(rec.styleName());
    assertEquals("Clickable", rec.variant());
    assertEquals("attackCard", rec.label()); // defaults to trigger
    assertEquals(0, rec.args().length);
    assertFalse(rec.hasSize());
    assertEquals(ButtonType.IMAGE, rec.type()); // no text -> IMAGE
  }

  @Test
  void builder_setsAllFieldsWhenProvided() {
    Skin skin = mock(Skin.class);

    ClickableRecord rec =
        ClickableRecord.builder("attackCard")
            .text("Attack")
            .skin(skin)
            .position(100.5f, 200.25f)
            .styleName("default")
            .size(64, 32)
            .variant("drag")
            .args(5, "poison", true)
            .label("Attack the enemy")
            .build();

    assertEquals("attackCard", rec.trigger());
    assertEquals("Attack", rec.text());
    assertEquals(skin, rec.btnSkin());
    assertEquals(100.5f, rec.x());
    assertEquals(200.25f, rec.y());
    assertEquals("default", rec.styleName());
    assertEquals("drag", rec.variant());
    assertEquals("Attack the enemy", rec.label());
    assertTrue(rec.hasSize());
    assertEquals(64f, rec.width());
    assertEquals(32f, rec.height());
    assertArrayEquals(new Object[] {5, "poison", true}, rec.args());
  }

  @Test
  void builder_argsCalledWithNothing_producesEmptyArrayNotNull() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").args().build();

    assertEquals(0, rec.args().length);
  }

  // --- Builder.build() type inference ---

  @Test
  void inferType_noTextNoSkin_returnsImage() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").build();

    assertEquals(ButtonType.IMAGE, rec.type());
  }

  @Test
  void inferType_noTextWithSkin_stillReturnsImage() {
    // text==null short-circuits before the skin check, regardless of skin being present.
    ClickableRecord rec = ClickableRecord.builder("attackCard").skin(mock(Skin.class)).build();

    assertEquals(ButtonType.IMAGE, rec.type());
  }

  @Test
  void inferType_textWithoutSkin_returnsText() {
    ClickableRecord rec = ClickableRecord.builder("attackCard").text("Attack").build();

    assertEquals(ButtonType.TEXT, rec.type());
  }

  @Test
  void inferType_textWithSkin_returnsImageText() {
    ClickableRecord rec =
        ClickableRecord.builder("attackCard").text("Attack").skin(mock(Skin.class)).build();

    assertEquals(ButtonType.IMAGE_TEXT, rec.type());
  }

  private static void assertArrayEquals(Object[] expected, Object[] actual) {
    org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
  }
}
