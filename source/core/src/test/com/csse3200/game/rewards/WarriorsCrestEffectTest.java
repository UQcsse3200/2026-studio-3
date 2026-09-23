package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class WarriorsCrestEffectTest {
  @Test
  void stacksBaseAttackForEachCopy() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);
    WarriorsCrestEffect effect = new WarriorsCrestEffect();

    effect.apply(player);
    effect.apply(player);

    assertEquals(7, stats.getBaseAttack());
  }
}
