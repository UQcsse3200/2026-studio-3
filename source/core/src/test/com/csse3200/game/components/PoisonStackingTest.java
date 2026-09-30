package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PoisonStackingTest {
  @Test
  void equalDurationsAddStacksWithoutExtendingLifetime() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.applyStatusEffect("POISON", 4, 2);
    assertEquals(Map.of(2, 7), stats.getPoisonStacksByDuration());
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    stats.processPoisonTick(hits::add);
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(7, 7), hits);
    assertNull(stats.getStatusEffect("POISON"));
  }

  @Test
  void differentDurationsExpireIndependentlyWithOneHitPerTick() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.applyStatusEffect("POISON", 4, 2);
    stats.applyStatusEffect("POISON", 5, 3);
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    assertEquals(Map.of(1, 7, 2, 5), stats.getPoisonStacksByDuration());
    stats.processPoisonTick(hits::add);
    assertEquals(Map.of(1, 5), stats.getPoisonStacksByDuration());
    assertEquals(5, stats.getStatusEffect("POISON").getValue());
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(12, 12, 5), hits);
    assertFalse(stats.hasStatusEffect("POISON"));
  }

  @Test
  void newlyAppliedPoisonMergesWithEqualRemainingDuration() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    stats.applyStatusEffect("POISON", 4, 1);
    assertEquals(Map.of(1, 7), stats.getPoisonStacksByDuration());
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(3, 7), hits);
    assertNull(stats.getStatusEffect("POISON"));
  }

  @Test
  void totalHitUsesDefensesAndTicksEvenWhenFullyBlocked() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.setBlock(3);
    stats.setArmour(9);
    stats.applyStatusEffect("POISON", 7, 1);
    stats.applyStatusEffect("POISON", 5, 2);
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(
        damage -> {
          hits.add(damage);
          stats.takeDamage(damage);
        });
    assertEquals(List.of(12), hits);
    assertEquals(100, stats.getHealth());
    assertEquals(0, stats.getBlock());
    assertEquals(0, stats.getArmour());
    assertEquals(Map.of(1, 5), stats.getPoisonStacksByDuration());
    stats.processPoisonTick(stats::takeDamage);
    assertEquals(95, stats.getHealth());
  }

  @Test
  void cleanseRemovesAllGroupsAndPreservesOtherState() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.applyStatusEffect("poison", 4, 3);
    stats.applyStatusEffect("STRENGTH", 2, 0);
    stats.clearNegativeStatusEffects();
    assertEquals(Map.of(), stats.getPoisonStacksByDuration());
    assertNull(stats.getStatusEffect("POISON"));
    assertNull(stats.getStatusEffect("poison"));
    assertEquals(2, stats.getStatusEffect("STRENGTH").getValue());
    stats.processPoisonTick(
        damage -> {
          throw new AssertionError("Cleansed poison hit");
        });
  }

  @Test
  void damageCallbackAdditionStartsOnNextTick() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.processPoisonTick(damage -> stats.applyStatusEffect("POISON", 4, 2));
    assertEquals(Map.of(1, 3, 2, 4), stats.getPoisonStacksByDuration());
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(7, 4), hits);
  }

  @Test
  void removalAndReapplicationDuringDamagePreservesNewPoison() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.processPoisonTick(
        damage -> {
          stats.removeStatusEffect("POISON");
          stats.applyStatusEffect("POISON", 7, 4);
        });
    assertEquals(Map.of(4, 7), stats.getPoisonStacksByDuration());
  }

  @Test
  void permanentPoisonSurvivesFinitePoisonExpiry() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 2, 0);
    stats.applyStatusEffect("POISON", 3, -1);
    stats.applyStatusEffect("POISON", 4, 1);
    assertEquals(0, stats.getStatusEffect("POISON").getDuration());
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(9, 5), hits);
    assertEquals(Map.of(0, 5), stats.getPoisonStacksByDuration());
  }

  @Test
  void largeStackTotalsDoNotOverflowIntoNegativeDamage() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", Integer.MAX_VALUE, 1);
    stats.applyStatusEffect("POISON", 1, 1);
    stats.applyStatusEffect("POISON", 2, 2);
    List<Integer> hits = new ArrayList<>();
    stats.processPoisonTick(hits::add);
    stats.processPoisonTick(hits::add);
    assertEquals(List.of(Integer.MAX_VALUE, 2), hits);
  }

  @Test
  void durationQueryIsImmutableAndAmbiguousMutationIsRejected() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.getStatusEffect("POISON").addValue(2);
    assertEquals(Map.of(2, 5), stats.getPoisonStacksByDuration());
    stats.applyStatusEffect("POISON", 4, 3);
    assertEquals(9, stats.getStatusEffect("POISON").getValue());
    assertEquals(3, stats.getStatusEffect("POISON").getDuration());
    assertThrows(
        UnsupportedOperationException.class, () -> stats.getPoisonStacksByDuration().put(2, 99));
    assertThrows(IllegalStateException.class, () -> stats.getStatusEffect("POISON").addValue(1));
  }
}
