package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Adds one base attack whenever the player's battle entity is created. */
public class WarriorsCrestEffect implements ItemEffect {
  private static final int ATTACK_BONUS = 1;

  @Override
  public void apply(Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setBaseAttack(stats.getBaseAttack() + ATTACK_BONUS);
  }
}
