package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChanceOutcomeTest {
  @Test
  void shouldKeepItemRewardsImmutableAndRetainDuplicateCopies() {
    List<String> items = new ArrayList<>(List.of("ENERGY_CRYSTAL", "ENERGY_CRYSTAL"));
    ChanceOutcome outcome = ChanceOutcome.withItemRewards(0, 0, items);
    items.clear();

    assertEquals(List.of("ENERGY_CRYSTAL", "ENERGY_CRYSTAL"), outcome.getItemRewardIds());
    assertFalse(outcome.isNoEffect());
    assertTrue(outcome.getCardRewardIds().isEmpty());
    assertThrows(UnsupportedOperationException.class, outcome.getItemRewardIds()::clear);
  }

  @Test
  void shouldRepresentCombinedRewardsWithoutChangingLegacyConstructors() {
    ChanceOutcome outcome =
        ChanceOutcome.withRewards(-5, 10, List.of("bandage"), List.of("LUCKY_COIN"));
    assertEquals(-5, outcome.getHealthDelta());
    assertEquals(10, outcome.getCurrencyDelta());
    assertEquals("bandage", outcome.getCardRewardId());
    assertEquals(List.of("LUCKY_COIN"), outcome.getItemRewardIds());
    assertTrue(new ChanceOutcome(1, 2, "bandage").getItemRewardIds().isEmpty());
    assertTrue(ChanceOutcome.withItemRewards(0, 0, List.of()).isNoEffect());
    assertThrows(NullPointerException.class, () -> ChanceOutcome.withItemRewards(0, 0, null));
  }

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

  @Test
  void shouldRepresentOptionalCardReward() {
    ChanceOutcome outcome = new ChanceOutcome(0, 0, "bandage");

    assertEquals("bandage", outcome.getCardRewardId());
    assertFalse(outcome.isNoEffect());
  }

  @Test
  void shouldKeepExistingOutcomesWithoutCardReward() {
    ChanceOutcome outcome = new ChanceOutcome(5, 10);

    assertNull(outcome.getCardRewardId());
  }

  @Test
  void shouldRepresentMultipleCardRewardsAndRetainDuplicates() {
    ChanceOutcome outcome = ChanceOutcome.withCardRewards(0, 15, List.of("bandage", "bandage"));
    List<String> rewardIds = outcome.getCardRewardIds();

    assertEquals("bandage", outcome.getCardRewardId());
    assertEquals(List.of("bandage", "bandage"), outcome.getCardRewardIds());
    assertThrows(UnsupportedOperationException.class, rewardIds::clear);
    assertFalse(outcome.isNoEffect());
  }
}
