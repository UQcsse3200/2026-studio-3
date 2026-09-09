package com.csse3200.game.components.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.bestiary.BestiaryEntry;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BestiaryDisplayTest {
  private final BestiaryEntry lockedNormal =
      createEntry("locked", "Hidden Enemy", EnemyTier.NORMAL, BestiaryUnlockState.LOCKED);
  private final BestiaryEntry unlockedElite =
      createEntry("elite", "Elite Enemy", EnemyTier.ELITE, BestiaryUnlockState.DEFEATED);
  private final BestiaryEntry lockedBoss =
      createEntry("boss", "Boss Enemy", EnemyTier.BOSS, BestiaryUnlockState.LOCKED);

  @Test
  void shouldFilterEntriesByTier() {
    List<BestiaryEntry> entries = List.of(lockedNormal, unlockedElite, lockedBoss);

    assertEquals(List.of(lockedNormal), BestiaryDisplay.filterEntries(entries, EnemyTier.NORMAL));
    assertEquals(List.of(unlockedElite), BestiaryDisplay.filterEntries(entries, EnemyTier.ELITE));
    assertEquals(List.of(lockedBoss), BestiaryDisplay.filterEntries(entries, EnemyTier.BOSS));
  }

  @Test
  void shouldHideLockedEnemyName() {
    assertEquals("???", BestiaryDisplay.visibleName(lockedNormal));
  }

  @Test
  void shouldShowUnlockedEnemyName() {
    assertEquals("Elite Enemy", BestiaryDisplay.visibleName(unlockedElite));
  }

  @Test
  void shouldHandleEmptyCategory() {
    assertEquals(List.of(), BestiaryDisplay.filterEntries(List.of(unlockedElite), EnemyTier.BOSS));
  }

  private BestiaryEntry createEntry(
      String id, String name, EnemyTier tier, BestiaryUnlockState state) {
    return new BestiaryEntry(
        id, name, tier, "Description", "images/enemies/default.png", 10, 2, 1, state);
  }
}
