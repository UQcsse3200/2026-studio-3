package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class ClickableJsonLoaderTest {

  @Test
  void parsesAllFieldsWhenPresent() {
    List<ClickableRecord> records =
        ClickableJsonLoader.loadRecordsFromJson(
            Path.of("spritedisplay/assestsForTest/clickable-basic.json"));

    assertEquals(1, records.size());
    ClickableRecord rec = records.get(0);

    assertEquals("attackCard", rec.trigger());
    assertEquals("Attack", rec.text());
    assertEquals(100.5f, rec.x());
    assertEquals(200.25f, rec.y());
    assertEquals("default", rec.styleName());
    assertEquals("drag", rec.variant());
    assertEquals("Attack the enemy", rec.label());
    assertTrue(rec.hasSize());
    assertEquals(64f, rec.width());
    assertEquals(32f, rec.height());
  }

  @Test
  void appliesDefaultsWhenOptionalFieldsOmitted() {
    List<ClickableRecord> records =
        ClickableJsonLoader.loadRecordsFromJson(
            Path.of("spritedisplay/assestsForTest/clickable-minimal.json"));

    assertEquals(1, records.size());
    ClickableRecord rec = records.get(0);

    assertEquals("healCard", rec.trigger());
    assertNull(rec.text());
    assertNull(rec.styleName());
    assertEquals("Clickable", rec.variant()); // DEFAULT_VARIANT
    assertEquals("healCard", rec.label()); // defaults to trigger
    assertFalse(rec.hasSize());
    assertEquals(0, rec.args().length);
    assertEquals(ClickableRecord.ButtonType.IMAGE, rec.type()); // no text -> IMAGE
    assertNull(rec.btnSkin()); // no skinFile/skinAtlas -> no skin loaded
  }

  @Test
  void parsesArgsWithCorrectTypes() {
    List<ClickableRecord> records =
        ClickableJsonLoader.loadRecordsFromJson(
            Path.of("spritedisplay/assestsForTest/clickable-args.json"));

    Object[] args = records.get(0).args();
    assertEquals(5, args.length);

    assertInstanceOfAndEquals(Integer.class, 5, args[0]); // whole-number int stays Integer
    assertInstanceOfAndEquals(Double.class, 2.5, args[1]); // fractional stays Double
    assertInstanceOfAndEquals(String.class, "poison", args[2]);
    assertInstanceOfAndEquals(Boolean.class, true, args[3]);
    assertInstanceOfAndEquals(Integer.class, 4, args[4]); // whole-number double -> Integer
  }

  @Test
  void parsesMultipleEntriesInOrder() {
    List<ClickableRecord> records =
        ClickableJsonLoader.loadRecordsFromJson(
            Path.of("spritedisplay/assestsForTest/clickable-multi.json"));
    ;

    assertEquals(3, records.size());
    assertEquals("cardOne", records.get(0).trigger());
    assertEquals("cardTwo", records.get(1).trigger());
    assertEquals("cardThree", records.get(2).trigger());
  }

  private static void assertInstanceOfAndEquals(
      Class<?> expectedType, Object expected, Object actual) {
    assertTrue(
        expectedType.isInstance(actual),
        "expected "
            + expectedType.getSimpleName()
            + " but got "
            + actual.getClass().getSimpleName());
    assertEquals(expected, actual);
  }
}
