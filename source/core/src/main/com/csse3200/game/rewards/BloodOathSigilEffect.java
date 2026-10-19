package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Curse effect: lose Health at the start of each player turn, never below 1. */
public class BloodOathSigilEffect implements ItemEffect {
  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }
    CursePenalties.addPenalty(stats, CursePenalties.BLOOD_OATH);
  }
}
