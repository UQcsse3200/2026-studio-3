package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.entities.Entity;

/** Curse effect: removes one Strength at the start of each battle. */
public class BrandOfTheDamnedEffect implements ItemEffect {
  private static final int STRENGTH_PENALTY = -1;
  private static final String STRENGTH = "STRENGTH";

  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }

    StatusEffect existingStrength = stats.getStatusEffect(STRENGTH);
    if (existingStrength == null) {
      stats.applyStatusEffect(STRENGTH, STRENGTH_PENALTY, 0);
    } else {
      existingStrength.addValue(STRENGTH_PENALTY);
    }
  }
}
