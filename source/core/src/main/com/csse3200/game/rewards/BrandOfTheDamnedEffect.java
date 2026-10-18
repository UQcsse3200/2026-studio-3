package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Curse effect: every Strength gain this battle is reduced by 1 per copy, never below 0. */
public class BrandOfTheDamnedEffect implements ItemEffect {
  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }
    CursePenalties.addPenalty(stats, CursePenalties.STRENGTH_PENALTY);
  }
}
