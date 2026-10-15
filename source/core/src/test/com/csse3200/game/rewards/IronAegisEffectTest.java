package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class IronAegisEffectTest {
  @Test
  void stacksArmourForEachCopy() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);
    IronAegisEffect effect = new IronAegisEffect();

    effect.apply(player);
    effect.apply(player);

    assertEquals(10, stats.getArmour());
  }
}
