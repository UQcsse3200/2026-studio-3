package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Grants five armour whenever the player's battle entity is created. */
public class IronAegisEffect implements ItemEffect {
  private static final int ARMOUR_BONUS = 5;

  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.addArmour(ARMOUR_BONUS);
  }
}
