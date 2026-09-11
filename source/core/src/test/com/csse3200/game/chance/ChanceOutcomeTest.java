package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChanceOutcomeTest {
  @Test
  void shouldRepresentPositiveHealthChange() {
    ChanceOutcome outcome = new ChanceOutcome(10, 0);

    assertEquals(10, outcome.getHealthDelta());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldRepresentNegativeHealthChange() {
    ChanceOutcome outcome = new ChanceOutcome(-10, 0);

    assertEquals(-10, outcome.getHealthDelta());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldRepresentPositiveCurrencyChange() {
    ChanceOutcome outcome = new ChanceOutcome(0, 20);

    assertEquals(20, outcome.getCurrencyDelta());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldRepresentNegativeCurrencyChange() {
    ChanceOutcome outcome = new ChanceOutcome(0, -20);

    assertEquals(-20, outcome.getCurrencyDelta());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldRepresentCombinedHealthAndCurrencyChanges() {
    ChanceOutcome outcome = new ChanceOutcome(-10, 25);

    assertEquals(-10, outcome.getHealthDelta());
    assertEquals(25, outcome.getCurrencyDelta());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldRepresentNoEffectWhenBothChangesAreZero() {
    ChanceOutcome outcome = new ChanceOutcome(0, 0);

    assertEquals(0, outcome.getHealthDelta());
    assertEquals(0, outcome.getCurrencyDelta());
    assertTrue(outcome.isNoEffect());
  }
}
