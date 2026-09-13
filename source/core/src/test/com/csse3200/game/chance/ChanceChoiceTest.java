package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChanceChoiceTest {
  @Test
  void shouldPreserveChoiceId() {
    ChanceChoice choice = new ChanceChoice("leave", "Leave the shrine.", new ChanceOutcome(0, 0));

    assertEquals("leave", choice.getId());
  }

  @Test
  void shouldPreserveChoiceDescription() {
    ChanceChoice choice =
        new ChanceChoice("sacrifice", "Sacrifice health.", new ChanceOutcome(-10, 25));

    assertEquals("Sacrifice health.", choice.getDescription());
  }

  @Test
  void shouldResolveAssociatedOutcome() {
    ChanceOutcome outcome = new ChanceOutcome(10, 0);
    ChanceChoice choice = new ChanceChoice("heal", "Accept the blessing.", outcome);

    assertSame(outcome, choice.resolve());
  }

  @Test
  void shouldResolveNoEffectOutcome() {
    ChanceChoice choice = new ChanceChoice("leave", "Leave the shrine.", new ChanceOutcome(0, 0));

    assertTrue(choice.resolve().isNoEffect());
  }

  @Test
  void shouldResolveCombinedHealthAndCurrencyOutcome() {
    ChanceChoice choice =
        new ChanceChoice("sacrifice", "Sacrifice health.", new ChanceOutcome(-10, 25));

    ChanceOutcome outcome = choice.resolve();

    assertEquals(-10, outcome.getHealthDelta());
    assertEquals(25, outcome.getCurrencyDelta());
  }
}
