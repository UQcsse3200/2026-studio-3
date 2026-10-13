package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.entities.Entity;

/** Grants one Strength for the current battle when one Warrior's Crest is actively used. */
public class WarriorsCrestEffect implements ItemEffect {
  private static final int STRENGTH_BONUS = 1;
  private static final String STRENGTH = "STRENGTH";

  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }

    int bonus = CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, STRENGTH_BONUS);
    if (bonus == 0) {
      return;
    }

    StatusEffect existingStrength = stats.getStatusEffect(STRENGTH);
    if (existingStrength == null) {
      stats.applyStatusEffect(STRENGTH, bonus, 0);
    } else {
      existingStrength.addValue(bonus);
    }
  }
}
