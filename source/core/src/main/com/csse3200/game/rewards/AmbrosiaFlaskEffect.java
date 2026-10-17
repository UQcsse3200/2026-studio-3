package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Restores twenty health when one Ambrosia Flask is actively used during battle. */
public class AmbrosiaFlaskEffect implements ItemEffect {
  public static final int HEALTH_RESTORED = 20;

  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }
    stats.heal(HEALTH_RESTORED);
  }
}
