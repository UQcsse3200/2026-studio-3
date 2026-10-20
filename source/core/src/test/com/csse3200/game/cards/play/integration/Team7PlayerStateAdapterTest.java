package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.CursePenalties;
import java.util.Collections;
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
  void shouldStackCardStrengthOnExistingItemStrength() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.applyStatusEffect(EffectType.STRENGTH.name(), 1, 0);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    adapter.applyPlayerEffects(List.of(effect(EffectType.STRENGTH, 2, 0, 0)));

    assertEquals(3, stats.getStatusEffect(EffectType.STRENGTH.name()).getValue());
  }

  @Test
  void curseItemsReduceCardGainsByTheirActualStackCount() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    CursePenalties.addPenalty(stats, CursePenalties.STRENGTH_PENALTY);
    CursePenalties.addPenalty(stats, CursePenalties.BLOCK_PENALTY);
    CursePenalties.addPenalty(stats, CursePenalties.BLOCK_PENALTY);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    adapter.applyPlayerEffects(
        List.of(effect(EffectType.STRENGTH, 3, 0, 0), effect(EffectType.BLOCK, 4, 0, 1)));

    assertEquals(2, stats.getStatusEffect(EffectType.STRENGTH.name()).getValue());
    assertEquals(2, stats.getBlock());
  }

  @Test
  void shouldClearDebuffsAndKeepStrengthWhenCleansing() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.applyStatusEffect(EffectType.POISON.name(), 3, 2);
    stats.applyStatusEffect("vulnerable", 1, 1);
    stats.applyStatusEffect(EffectType.STRENGTH.name(), 2, 0);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    adapter.applyPlayerEffects(List.of(effect(EffectType.CLEANSE, 1, 0, 0)));

    assertFalse(stats.hasStatusEffect(EffectType.POISON.name()));
    assertFalse(stats.hasStatusEffect("vulnerable"));
    assertTrue(stats.hasStatusEffect(EffectType.STRENGTH.name()));
    assertEquals(2, stats.getStatusEffect(EffectType.STRENGTH.name()).getValue());
  }

  @Test
  void shouldRestoreEnergyWithoutExceedingMaximumWhenApplyingEnergyGain() {
    EnergyComponent energy = new EnergyComponent(3);
    energy.spendEnergy(2);
    Team7PlayerStateAdapter adapter =
        new Team7PlayerStateAdapter(energy, new CombatStatsComponent(10, 1));

    adapter.applyPlayerEffects(List.of(effect(EffectType.ENERGY_GAIN, 5, 0, 0)));

    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void shouldApplyFortifyAsArmourWithoutChangingBlock() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    adapter.applyPlayerEffects(List.of(effect(EffectType.FORTIFY, 4, 0, 0)));

    assertEquals(4, stats.getArmour());
    assertEquals(0, stats.getBlock());
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

  @Test
  void entityConstructorRequiresBothPlayerComponents() {
    Entity complete =
        new Entity()
            .addComponent(new EnergyComponent(3))
            .addComponent(new CombatStatsComponent(10, 1));
    Entity withoutComponents = new Entity();
    Entity energyOnly = new Entity().addComponent(new EnergyComponent(3));
    CombatStatsComponent combatStats = new CombatStatsComponent(10, 1);
    EnergyComponent energy = new EnergyComponent(3);
    assertEquals(3, new Team7PlayerStateAdapter(complete).currentEnergy());
    assertThrows(IllegalArgumentException.class, () -> new Team7PlayerStateAdapter((Entity) null));
    assertThrows(
        IllegalArgumentException.class, () -> new Team7PlayerStateAdapter(withoutComponents));
    assertThrows(IllegalArgumentException.class, () -> new Team7PlayerStateAdapter(energyOnly));
    assertThrows(
        IllegalArgumentException.class, () -> new Team7PlayerStateAdapter(null, combatStats));
    assertThrows(IllegalArgumentException.class, () -> new Team7PlayerStateAdapter(energy, null));
  }

  @Test
  void rejectsNullAndNonPlayerEffectsBeforeApplyingAnyOfThem() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    assertThrows(IllegalArgumentException.class, () -> adapter.statusValue(null));
    assertThrows(IllegalArgumentException.class, () -> adapter.applyPlayerEffects(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> adapter.applyPlayerEffects(Collections.singletonList(null)));
    List<ResolvedCardEffect> mixedEffects =
        List.of(
            effect(EffectType.BLOCK, 5, 0, 0),
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 2, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> adapter.applyPlayerEffects(mixedEffects));
    assertEquals(0, stats.getBlock());
  }

  @Test
  void healingOverTimeCreatesAStatusInsteadOfHealingImmediately() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 1);
    stats.setHealth(4);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(new EnergyComponent(3), stats);

    adapter.applyPlayerEffects(List.of(effect(EffectType.HEAL, 2, 2, 0)));

    assertEquals(4, stats.getHealth());
    assertTrue(stats.hasStatusEffect(EffectType.HEAL.name()));
  }

  private static ResolvedCardEffect effect(EffectType type, int value, int duration, int sequence) {
    return new ResolvedCardEffect("player_card", type, TargetType.SELF, value, duration, sequence);
  }
}
