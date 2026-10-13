package com.csse3200.game.components.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StatusInfoTest {

  @Test
  void shouldFindEveryStatusByItsStoredKey() {
    for (StatusInfo info : StatusInfo.values()) {
      assertEquals(Optional.of(info), StatusInfo.forKey(info.name()));
    }
  }

  @Test
  void shouldIgnoreTheEnemySuffixOnATauntKey() {
    assertEquals(Optional.of(StatusInfo.TAUNT), StatusInfo.forKey("TAUNT:12"));
  }

  @Test
  void shouldMatchKeysRegardlessOfCase() {
    assertEquals(Optional.of(StatusInfo.POISON), StatusInfo.forKey("poison"));
  }

  @Test
  void shouldReturnEmptyForMissingOrUnknownKeys() {
    assertTrue(StatusInfo.forKey(null).isEmpty());
    assertTrue(StatusInfo.forKey("").isEmpty());
    assertTrue(StatusInfo.forKey("   ").isEmpty());
    assertTrue(StatusInfo.forKey(":12").isEmpty());
    assertTrue(StatusInfo.forKey("BLOCK").isEmpty());
  }

  @Test
  void shouldInsertTheValueIntoDescriptionsThatMentionAnAmount() {
    assertEquals(
        "Takes 3 damage each time a card is played.", StatusInfo.DAMAGE_ON_CARD_PLAY.describe(3));
  }

  @Test
  void shouldIgnoreTheValueForDescriptionsWithoutAnAmount() {
    assertEquals("Cannot play cards.", StatusInfo.SILENCE.describe(7));
  }

  @Test
  void shouldDescribeFiniteAndPermanentDurations() {
    assertEquals("1 turn remaining", StatusInfo.durationText(1));
    assertEquals("2 turns remaining", StatusInfo.durationText(2));
    assertEquals("Lasts until the end of combat", StatusInfo.durationText(0));
    assertEquals("Lasts until the end of combat", StatusInfo.durationText(-1));
  }

  @Test
  void shouldGiveEveryStatusATitleDescriptionAndPngIcon() {
    for (StatusInfo info : StatusInfo.values()) {
      assertFalse(info.title().isBlank(), info.name());
      assertFalse(info.describe(1).isBlank(), info.name());
      assertTrue(info.iconPath().endsWith(".png"), info.name());
    }
  }

  @Test
  void shouldListEachIconPathOnce() {
    String[] paths = StatusInfo.iconPaths();
    Set<String> unique = new HashSet<>(Arrays.asList(paths));

    assertEquals(unique.size(), paths.length);
    for (StatusInfo info : StatusInfo.values()) {
      assertTrue(unique.contains(info.iconPath()), info.name());
    }
  }
}
