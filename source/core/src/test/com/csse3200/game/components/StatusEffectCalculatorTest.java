package com.csse3200.game.components;

import static com.csse3200.game.components.StatusEffectCalculator.getIncomingDamageModifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatusEffectCalculatorTest {

  // Feeble
  @Test
  void shouldReturnNormalOutgoingModifierWithoutFeeble() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    assertEquals(1.0f, StatusEffectCalculator.getOutgoingDamageModifier(stats), 0.001f);
  }

  @Test
  void shouldReduceOutgoingDamageWhenFeebleIsActive() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("FEEBLE", 1, 2);
    assertEquals(0.75f, StatusEffectCalculator.getOutgoingDamageModifier(stats), 0.001f);
  }

  @Test
  void shouldIgnoreFeebleValueForModifier() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("FEEBLE", 5, 2);
    assertEquals(0.75f, StatusEffectCalculator.getOutgoingDamageModifier(stats), 0.001f);
  }

  @Test
  void shouldRestoreOutgoingModifierAfterFeebleValueExpires() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("FEEBLE", 1, 1);
    stats.updateStatusEffects();
    assertEquals(1.0f, StatusEffectCalculator.getOutgoingDamageModifier(stats), 0.001f);
  }

  // Vulnerable
  @Test
  void shouldReturnNormalIncomingModifierWithoutVulnerable() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    assertEquals(1.0f, getIncomingDamageModifier(stats), 0.001f);
  }

  @Test
  void shouldIncreaseIncomingDamageWhenVulnerableIsActive() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("VULNERABLE", 2, 2);
    assertEquals(1.5f, getIncomingDamageModifier(stats), 0.001f);
  }

  @Test
  void shouldRestoreIncomingModifierAfterVulnerableIsExpires() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("VULNERABLE", 2, 1);
    stats.updateStatusEffects();
    assertEquals(1.0f, getIncomingDamageModifier(stats), 0.001f);
  }

  // Poison
  @Test
  void shouldReturnZeroPoisonDamageWithoutIsPoison() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    assertEquals(0, StatusEffectCalculator.getPoisonDamage(stats));
  }

  @Test
  void shouldReturnPoisonValueAsDamage() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("POISON", 3, 2);
    assertEquals(3, StatusEffectCalculator.getPoisonDamage(stats));
  }

  @Test
  void shouldReturnZeroPoisonDamageAfterPoisonExpires() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("POISON", 3, 1);
    stats.updateStatusEffects();
    assertEquals(0, StatusEffectCalculator.getPoisonDamage(stats));
  }
}
