package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class CurseItemEffectsTest {
  @Test
  void repeatedBrandAndAegisCopiesReduceGainsButNeverBelowZero() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);

    new BrandOfTheDamnedEffect().apply(player);
    new BrandOfTheDamnedEffect().apply(player);
    new ShatteredAegisEffect().apply(player);
    new ShatteredAegisEffect().apply(player);

    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.STRENGTH_PENALTY));
    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.BLOCK_PENALTY));
    assertEquals(1, CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, 3));
    assertEquals(0, CursePenalties.reduce(stats, CursePenalties.BLOCK_PENALTY, 1));
  }

  @Test
  void repeatedBloodOathCopiesIncreaseTheStartOfTurnPenalty() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);

    new BloodOathSigilEffect().apply(player);
    new BloodOathSigilEffect().apply(player);

    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH));
    assertEquals(
        4,
        CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH)
            * CursePenalties.BLOOD_OATH_DAMAGE_PER_COPY);
  }

  @Test
  void battleCurseEffectsRejectPlayersWithoutCombatStats() {
    Entity player = new Entity();

    assertThrows(IllegalArgumentException.class, () -> new BrandOfTheDamnedEffect().apply(player));
    assertThrows(IllegalArgumentException.class, () -> new ShatteredAegisEffect().apply(player));
    assertThrows(IllegalArgumentException.class, () -> new BloodOathSigilEffect().apply(player));
  }
}
