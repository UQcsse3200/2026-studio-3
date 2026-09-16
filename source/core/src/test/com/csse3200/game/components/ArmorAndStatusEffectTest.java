package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ArmorAndStatusEffectTest {

  // -----------------------------------------------------------------
  // StatusEffect class itself
  // -----------------------------------------------------------------

  @Test
  void shouldStoreInitialStatusEffectValues() {
    StatusEffect effect = new StatusEffect("VULNERABLE", 2, 2);
    assertEquals("VULNERABLE", effect.getType());
    assertEquals(2, effect.getValue());
    assertEquals(2, effect.getDuration());
  }

  @Test
  void statusEffectShouldExpireAfterDurationReachesZero() {
    StatusEffect effect = new StatusEffect("VULNERABLE", 2, 2);
    assertFalse(effect.tickAndCheckExpired());
    assertTrue(effect.tickAndCheckExpired());
  }

  @Test
  void statusEffectWithZeroDurationShouldBePermanent() {
    // e.g. STRENGTH, which Team 6 defines with duration=0 and lasts the whole combat.
    StatusEffect effect = new StatusEffect("STRENGTH", 2, 0);
    assertFalse(effect.tickAndCheckExpired());
    assertFalse(effect.tickAndCheckExpired());
    assertEquals(0, effect.getDuration());
  }

  @Test
  void shouldSetValueDirectly() {
    StatusEffect effect = new StatusEffect("POISON", 3, 3);
    effect.setValue(5);
    assertEquals(5, effect.getValue());
  }

  @Test
  void shouldAddPositiveAmountToValue() {
    // e.g. stacking more Poison onto an already-active effect
    StatusEffect effect = new StatusEffect("POISON", 3, 3);
    effect.addValue(2);
    assertEquals(5, effect.getValue());
  }

  @Test
  void shouldAddNegativeAmountToValue() {
    // e.g. a card that removes some stacks
    StatusEffect effect = new StatusEffect("POISON", 5, 3);
    effect.addValue(-2);
    assertEquals(3, effect.getValue());
  }

  // -----------------------------------------------------------------
  // Armor on CombatStatsComponent (permanent pool)
  // -----------------------------------------------------------------

  @Test
  void shouldSetGetArmor() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(0, combat.getArmor());
    combat.setArmor(10);
    assertEquals(10, combat.getArmor());
    combat.setArmor(-5);
    assertEquals(0, combat.getArmor());
  }

  @Test
  void shouldAddArmor() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addArmor(5);
    assertEquals(5, combat.getArmor());
    combat.addArmor(-100);
    assertEquals(5, combat.getArmor());
  }

  @Test
  void shouldClearArmor() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addArmor(15);
    combat.clearArmor();
    assertEquals(0, combat.getArmor());
  }

  @Test
  void armorShouldAbsorbDamageBeforeHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addArmor(5);
    combat.takeDamage(8);
    assertEquals(0, combat.getArmor());
    assertEquals(97, combat.getHealth());
  }

  @Test
  void armorShouldFullyAbsorbDamageWhenSufficient() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addArmor(10);
    combat.takeDamage(6);
    assertEquals(4, combat.getArmor());
    assertEquals(100, combat.getHealth());
  }

  @Test
  void damageShouldHitHealthDirectlyWhenNoArmorOrBlock() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.takeDamage(30);
    assertEquals(0, combat.getArmor());
    assertEquals(70, combat.getHealth());
  }

  // -----------------------------------------------------------------
  // Block on CombatStatsComponent (per-turn pool)
  // -----------------------------------------------------------------

  @Test
  void shouldSetGetBlock() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(0, combat.getBlock());
    combat.setBlock(10);
    assertEquals(10, combat.getBlock());
    combat.setBlock(-5);
    assertEquals(0, combat.getBlock());
  }

  @Test
  void shouldAddBlock() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(5);
    assertEquals(5, combat.getBlock());
    combat.addBlock(-100);
    assertEquals(5, combat.getBlock());
  }

  @Test
  void resetBlockShouldClearBlockRegardlessOfValue() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(15);
    combat.resetBlock();
    assertEquals(0, combat.getBlock());
  }

  @Test
  void blockShouldAbsorbDamageBeforeHealthWhenNoArmor() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(5);
    combat.takeDamage(8);
    assertEquals(0, combat.getBlock());
    assertEquals(97, combat.getHealth());
  }

  @Test
  void blockShouldFullyAbsorbDamageWhenSufficient() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(10);
    combat.takeDamage(6);
    assertEquals(4, combat.getBlock());
    assertEquals(100, combat.getHealth());
  }

  // -----------------------------------------------------------------
  // Block + Armor combined in takeDamage()
  // -----------------------------------------------------------------

  @Test
  void blockShouldAbsorbBeforeArmor() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(3);
    combat.addArmor(4);
    combat.takeDamage(10);
    assertEquals(0, combat.getBlock());
    assertEquals(0, combat.getArmor());
    assertEquals(97, combat.getHealth());
  }

  @Test
  void blockAndArmorTogetherShouldFullyAbsorbDamageWhenSufficient() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBlock(5);
    combat.addArmor(10);
    combat.takeDamage(8);
    assertEquals(0, combat.getBlock());
    assertEquals(7, combat.getArmor());
    assertEquals(100, combat.getHealth());
  }

  // -----------------------------------------------------------------
  // Status effect management on CombatStatsComponent
  // -----------------------------------------------------------------

  @Test
  void shouldApplyAndQueryStatusEffect() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertFalse(combat.hasStatusEffect("VULNERABLE"));

    StatusEffect vulnerable = new StatusEffect("VULNERABLE", 2, 2);
    combat.applyStatusEffect(vulnerable);

    assertTrue(combat.hasStatusEffect("VULNERABLE"));
    assertEquals(vulnerable, combat.getStatusEffect("VULNERABLE"));
  }

  @Test
  void shouldApplyStatusEffectUsingThreeArgOverload() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect("VULNERABLE", 2, 2);

    assertTrue(combat.hasStatusEffect("VULNERABLE"));
    assertEquals(2, combat.getStatusEffect("VULNERABLE").getValue());
    assertEquals(2, combat.getStatusEffect("VULNERABLE").getDuration());
  }

  @Test
  void applyingSameTypeShouldOverwritePrevious() {
    // Design decision: applyStatusEffect() itself overwrites, not stacks, by default.
    // Callers that want stacking behaviour (e.g. Poison) should read the existing effect via
    // getStatusEffect() and call addValue() on it themselves before re-applying, or call
    // addValue() directly on the existing instance.
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect(new StatusEffect("VULNERABLE", 2, 2));
    combat.applyStatusEffect(new StatusEffect("VULNERABLE", 2, 5));

    assertEquals(5, combat.getStatusEffect("VULNERABLE").getDuration());
  }

  @Test
  void callerCanStackByMutatingExistingEffect() {
    // Demonstrates how a caller (e.g. the teammate implementing Poison) can achieve stacking
    // using the mutable value, without CombatStatsComponent needing to know about stacking.
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect(new StatusEffect("POISON", 3, 3));

    StatusEffect existing = combat.getStatusEffect("POISON");
    existing.addValue(2);

    assertEquals(5, combat.getStatusEffect("POISON").getValue());
  }

  @Test
  void shouldExplicitlyRemoveStatusEffect() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect(new StatusEffect("VULNERABLE", 2, 2));
    combat.removeStatusEffect("VULNERABLE");

    assertFalse(combat.hasStatusEffect("VULNERABLE"));
    assertNull(combat.getStatusEffect("VULNERABLE"));
  }

  @Test
  void statusEffectShouldBeAutoRemovedAfterDurationExpires() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect(new StatusEffect("VULNERABLE", 2, 2));

    combat.updateStatusEffects();
    assertTrue(combat.hasStatusEffect("VULNERABLE"));

    combat.updateStatusEffects();
    assertFalse(combat.hasStatusEffect("VULNERABLE"));
  }

  @Test
  void permanentStatusEffectShouldNotBeRemovedByUpdateStatusEffects() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.applyStatusEffect(new StatusEffect("STRENGTH", 2, 0));

    combat.updateStatusEffects();
    assertTrue(combat.hasStatusEffect("STRENGTH"));

    combat.updateStatusEffects();
    assertTrue(combat.hasStatusEffect("STRENGTH"));
  }

  /** Verifies that reducing armor does not modify the entity's block or health. */
  @Test
  void reduceArmorShouldNotDamageBlockOrHealth() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setArmor(5);
    stats.setBlock(4);
    assertEquals(3, stats.reduceArmor(3));
    assertEquals(2, stats.getArmor());
    assertEquals(4, stats.getBlock());
    assertEquals(100, stats.getHealth());
    assertFalse(stats.hasStatusEffect("SUNDER"));
  }

  /** Verifies that armor cannot fall below zero and non-positive reductions are ignored. */
  @Test
  void reduceArmorShouldClampAndIgnoreNonPositiveAmounts() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setArmor(2);
    assertEquals(0, stats.reduceArmor(-3));
    assertEquals(0, stats.reduceArmor(0));
    assertEquals(2, stats.getArmor());
    assertEquals(2, stats.reduceArmor(Integer.MAX_VALUE));
    assertEquals(0, stats.getArmor());
    assertEquals(0, stats.reduceArmor(3));
  }

  /**
   * Verifies that one poison tick applies damage and updates POISON without changing unrelated
   * status effects.
   */
  @Test
  void poisonShouldTickBeforeExpiryWithoutTickingOtherEffects() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.applyStatusEffect("HEAL", 2, 3);
    java.util.List<Integer> damage = new java.util.ArrayList<>();
    stats.processPoisonTick(damage::add);
    assertEquals(1, stats.getStatusEffect("POISON").getDuration());
    stats.processPoisonTick(damage::add);
    stats.processPoisonTick(damage::add);
    assertEquals(java.util.List.of(3, 3), damage);
    assertNull(stats.getStatusEffect("POISON"));
    assertEquals(3, stats.getStatusEffect("HEAL").getDuration());
    assertEquals(100, stats.getHealth());
  }

  /** Verifies that poison uses the supplied damage handler and does not tick a dead entity. */
  @Test
  void poisonShouldUseSuppliedDamageHandlerAndSkipDeadEntities() {
    CombatStatsComponent stats = new CombatStatsComponent(3, 1);
    stats.applyStatusEffect("POISON", 3, 1);
    stats.processPoisonTick(stats::takeDamage);
    assertTrue(stats.isDead());
    assertNull(stats.getStatusEffect("POISON"));
    stats.applyStatusEffect("POISON", 3, 2);
    stats.processPoisonTick(
        value -> {
          throw new AssertionError("Dead entity ticked");
        });
    assertEquals(2, stats.getStatusEffect("POISON").getDuration());
  }

  /** Verifies that a POISON effect replaced during damage handling is not immediately ticked. */
  @Test
  void poisonShouldPreserveReplacementCreatedDuringDamageCallback() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("POISON", 3, 1);
    stats.processPoisonTick(value -> stats.applyStatusEffect("POISON", 7, 4));
    assertEquals(7, stats.getStatusEffect("POISON").getValue());
    assertEquals(4, stats.getStatusEffect("POISON").getDuration());
  }

  /** Verifies that non-positive poison applies no damage but still expires normally. */
  @Test
  void nonPositivePoisonShouldExpireWithoutApplyingDamage() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.applyStatusEffect("POISON", -2, 1);
    stats.processPoisonTick(
        value -> {
          throw new AssertionError("Negative poison damage");
        });
    assertNull(stats.getStatusEffect("POISON"));
  }

  /** Verifies that cleanse removes supported debuffs regardless of key case. */
  @Test
  void cleanseShouldRemoveSupportedDebuffsAndPreserveOtherState() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setArmor(5);
    stats.setBlock(4);
    stats.applyStatusEffect("POISON", 3, 2);
    stats.applyStatusEffect("poison", 2, 1);
    stats.applyStatusEffect("Vulnerable", 1, 2);
    stats.applyStatusEffect("feeble", 1, 3);

    StatusEffect strength = new StatusEffect("STRENGTH", 2, 0);
    StatusEffect healing = new StatusEffect("HEAL", 2, 3);
    StatusEffect other = new StatusEffect("CUSTOM_EFFECT", 1, 2);
    stats.applyStatusEffect(strength);
    stats.applyStatusEffect(healing);
    stats.applyStatusEffect(other);

    stats.clearNegativeStatusEffects();

    assertNull(stats.getStatusEffect("POISON"));
    assertNull(stats.getStatusEffect("poison"));
    assertNull(stats.getStatusEffect("Vulnerable"));
    assertNull(stats.getStatusEffect("feeble"));
    assertEquals(strength, stats.getStatusEffect("STRENGTH"));
    assertEquals(healing, stats.getStatusEffect("HEAL"));
    assertEquals(other, stats.getStatusEffect("CUSTOM_EFFECT"));
    assertEquals(3, healing.getDuration());
    assertEquals(2, other.getDuration());
    assertEquals(100, stats.getHealth());
    assertEquals(5, stats.getArmor());
    assertEquals(4, stats.getBlock());
  }

  /** Verifies that cleansing an empty or already-cleansed component is harmless. */
  @Test
  void cleanseShouldBeSafeWhenRepeated() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);

    stats.clearNegativeStatusEffects();

    stats.applyStatusEffect("POISON", 3, 2);
    stats.clearNegativeStatusEffects();
    stats.clearNegativeStatusEffects();

    assertNull(stats.getStatusEffect("POISON"));
    assertEquals(100, stats.getHealth());
  }

  /** Verifies that piercing damage bypasses defenses and ignores status damage modifiers. */
  @Test
  void piercingDamageShouldBypassDefensesAndIgnoreModifiers() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setBlock(6);
    stats.setArmor(8);
    stats.applyStatusEffect("STRENGTH", 5, 0);
    stats.applyStatusEffect("VULNERABLE", 1, 2);
    stats.applyStatusEffect("FEEBLE", 1, 2);

    stats.takePiercingDamage(7);

    assertEquals(93, stats.getHealth());
    assertEquals(6, stats.getBlock());
    assertEquals(8, stats.getArmor());
    assertEquals(5, stats.getStatusEffect("STRENGTH").getValue());
    assertEquals(2, stats.getStatusEffect("VULNERABLE").getDuration());
    assertEquals(2, stats.getStatusEffect("FEEBLE").getDuration());
    assertFalse(stats.hasStatusEffect("PIERCE"));
  }

  /** Verifies that each piercing hit applies independently and cannot reduce health below zero. */
  @Test
  void piercingDamageShouldApplyRepeatedlyAndClampHealth() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 0);

    stats.takePiercingDamage(3);
    stats.takePiercingDamage(3);

    assertEquals(4, stats.getHealth());

    stats.takePiercingDamage(Integer.MAX_VALUE);

    assertEquals(0, stats.getHealth());
    assertTrue(stats.isDead());
  }

  /** Verifies that non-positive piercing damage does not change health or defenses. */
  @Test
  void nonPositivePiercingDamageShouldNotChangeCombatValues() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 0);
    stats.setArmor(5);
    stats.setBlock(4);

    stats.takePiercingDamage(-3);
    stats.takePiercingDamage(0);

    assertEquals(20, stats.getHealth());
    assertEquals(5, stats.getArmor());
    assertEquals(4, stats.getBlock());
  }

  /** Verifies that piercing damage reports health changes and reports death only once. */
  @Test
  void piercingDamageShouldNotifyHealthAndDeathOnce() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 0);
    Entity entity = new Entity().addComponent(stats);
    int[] healthUpdates = {0};
    int[] reportedHealth = {-1};
    int[] reportedMaxHealth = {-1};
    int[] deathEvents = {0};

    entity
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health, Integer maxHealth) -> {
              healthUpdates[0]++;
              reportedHealth[0] = health;
              reportedMaxHealth[0] = maxHealth;
            });
    entity.getEvents().addListener("entityIsDead", () -> deathEvents[0]++);

    stats.takePiercingDamage(3);

    assertEquals(1, healthUpdates[0]);
    assertEquals(7, reportedHealth[0]);
    assertEquals(10, reportedMaxHealth[0]);
    assertEquals(0, deathEvents[0]);

    stats.takePiercingDamage(7);

    assertEquals(2, healthUpdates[0]);
    assertEquals(0, reportedHealth[0]);
    assertEquals(1, deathEvents[0]);

    stats.takePiercingDamage(5);

    assertEquals(2, healthUpdates[0]);
    assertEquals(1, deathEvents[0]);
  }
}
