package com.csse3200.game.bestiary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.configs.EnemyTier;
import org.junit.jupiter.api.Test;

class BestiaryEntryTest {
  @Test
  void shouldHideLockedEntries() {
    assertFalse(createEntry(BestiaryUnlockState.LOCKED).isUnlocked());
  }

  @Test
  void shouldRevealEncounteredAndDefeatedEntries() {
    assertTrue(createEntry(BestiaryUnlockState.ENCOUNTERED).isUnlocked());
    assertTrue(createEntry(BestiaryUnlockState.DEFEATED).isUnlocked());
  }

  @Test
  void shouldRejectBlankEnemyId() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BestiaryEntry(
                " ",
                "Enemy",
                EnemyTier.NORMAL,
                "Description",
                "image.png",
                10,
                2,
                0,
                BestiaryUnlockState.LOCKED));
  }

  private BestiaryEntry createEntry(BestiaryUnlockState state) {
    return new BestiaryEntry(
        "enemy", "Enemy", EnemyTier.NORMAL, "Description", "image.png", 10, 2, 0, state);
  }
}
