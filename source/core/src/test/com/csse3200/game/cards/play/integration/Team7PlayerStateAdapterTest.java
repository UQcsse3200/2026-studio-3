package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

class Team7PlayerStateAdapterTest {
  @Test
  void shouldReadEnergyAndCardModifierStatusesFromTeamSevenComponents() {
    EnergyComponent energy = new EnergyComponent(3);
    energy.spendEnergy(1);
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.applyStatusEffect(EffectType.STRENGTH.name(), 2, 0);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(energy, stats);

    assertEquals(2, adapter.currentEnergy());
    assertEquals(2, adapter.statusValue(EffectType.STRENGTH));
    assertEquals(0, adapter.statusValue(EffectType.FEEBLE));
  }

  @Test
  void shouldApplyBlockHealAndStrengthInSequenceOrder() {
    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.setHealth(4);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(energy, stats);

    adapter.applyPlayerEffects(
        List.of(
            effect(EffectType.STRENGTH, 3, 0, 2),
            effect(EffectType.HEAL, 4, 0, 1),
            effect(EffectType.BLOCK, 5, 0, 0)));

    assertEquals(5, stats.getBlock());
    assertEquals(8, stats.getHealth());
    assertEquals(3, stats.getStatusEffect(EffectType.STRENGTH.name()).getValue());
  }

  @Test
  void shouldRejectUnsupportedEffectsBeforeChangingPlayerState() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    List<ResolvedCardEffect> effects =
        List.of(
            effect(EffectType.BLOCK, 5, 0, 0),
            new ResolvedCardEffect("mixed", EffectType.DAMAGE, TargetType.SELF, 2, 0, 1));

    assertThrows(IllegalArgumentException.class, () -> adapter.applyPlayerEffects(effects));
    assertEquals(0, stats.getBlock());
  }

  private static ResolvedCardEffect effect(EffectType type, int value, int duration, int sequence) {
    return new ResolvedCardEffect("player_card", type, TargetType.SELF, value, duration, sequence);
  }
}
