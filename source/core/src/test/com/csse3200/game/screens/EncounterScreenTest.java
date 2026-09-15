package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class EncounterScreenTest {

  @Test
  void shouldDetectZeroHealthPlayerAsDefeated() {
    CombatStatsComponent stats = new CombatStatsComponent(5, 1);
    stats.setHealth(0);
    Entity player = new Entity().addComponent(stats);

    assertTrue(EncounterScreen.isPlayerDefeated(player));
  }

  @Test
  void shouldNotDetectPositiveHealthPlayerAsDefeated() {
    CombatStatsComponent stats = new CombatStatsComponent(5, 1);
    Entity player = new Entity().addComponent(stats);

    assertFalse(EncounterScreen.isPlayerDefeated(player));
  }

  @Test
  void shouldSafelyHandleMissingPlayerStats() {
    assertFalse(EncounterScreen.isPlayerDefeated(null));
    assertFalse(EncounterScreen.isPlayerDefeated(new Entity()));
  }
}
