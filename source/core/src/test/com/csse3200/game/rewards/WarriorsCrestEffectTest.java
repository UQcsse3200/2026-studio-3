package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.effects.CardEffectResolutionContext;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class WarriorsCrestEffectTest {
  @Test
  void stacksStrengthForEachCopy() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);
    WarriorsCrestEffect effect = new WarriorsCrestEffect();

    effect.apply(player);
    effect.apply(player);

    assertEquals(2, stats.getStatusEffect("STRENGTH").getValue());
    assertEquals(5, stats.getBaseAttack());

    Team7PlayerStateAdapter playerState =
        new Team7PlayerStateAdapter(new EnergyComponent(3), stats);
    CardEffectResolutionContext context =
        new CardEffectResolutionContext(playerState.statusValue(EffectType.STRENGTH), 0, 0);
    assertEquals(8, context.resolveDamage(6));
  }
}
