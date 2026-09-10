package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChanceEncounterTest {
  @Test
  void shouldPreserveEncounterId() {
    ChanceEncounter encounter = createShrineEncounter();

    assertEquals("mysterious-shrine", encounter.getId());
  }

  @Test
  void shouldPreserveEncounterDescription() {
    ChanceEncounter encounter = createShrineEncounter();

    assertEquals("You find a mysterious shrine.", encounter.getDescription());
  }

  @Test
  void shouldPreserveChoiceOrder() {
    ChanceChoice sacrifice = sacrificeChoice();
    ChanceChoice leave = leaveChoice();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "mysterious-shrine", "You find a mysterious shrine.", List.of(sacrifice, leave));

    assertSame(sacrifice, encounter.getChoices().get(0));
    assertSame(leave, encounter.getChoices().get(1));
  }

  @Test
  void shouldDefensivelyCopyChoices() {
    List<ChanceChoice> choices = new ArrayList<>(List.of(sacrificeChoice(), leaveChoice()));
    ChanceEncounter encounter =
        new ChanceEncounter("mysterious-shrine", "You find a mysterious shrine.", choices);

    choices.clear();

    assertEquals(2, encounter.getChoices().size());
  }

  @Test
  void shouldExposeReadOnlyChoices() {
    ChanceEncounter encounter = createShrineEncounter();

    assertThrows(UnsupportedOperationException.class, () -> encounter.getChoices().clear());
  }

  @Test
  void shouldResolveChoiceById() {
    ChanceChoice sacrifice = sacrificeChoice();
    ChanceEncounter encounter =
        new ChanceEncounter(
            "mysterious-shrine",
            "You find a mysterious shrine.",
            List.of(sacrifice, leaveChoice()));

    assertSame(sacrifice.resolve(), encounter.resolveChoice("sacrifice"));
  }

  @Test
  void shouldReturnNullForUnknownChoiceId() {
    ChanceEncounter encounter = createShrineEncounter();

    assertNull(encounter.resolveChoice("missing"));
  }

  @Test
  void shouldReturnNullForNullChoiceId() {
    ChanceEncounter encounter = createShrineEncounter();

    assertNull(encounter.resolveChoice(null));
  }

  @Test
  void shouldResolveNoEffectOutcome() {
    ChanceEncounter encounter = createShrineEncounter();

    assertTrue(encounter.resolveChoice("leave").isNoEffect());
  }

  @Test
  void shouldResolveCombinedHealthAndCurrencyOutcome() {
    ChanceEncounter encounter = createShrineEncounter();

    ChanceOutcome outcome = encounter.resolveChoice("sacrifice");

    assertEquals(-10, outcome.getHealthDelta());
    assertEquals(25, outcome.getCurrencyDelta());
  }

  private static ChanceEncounter createShrineEncounter() {
    return new ChanceEncounter(
        "mysterious-shrine",
        "You find a mysterious shrine.",
        List.of(sacrificeChoice(), leaveChoice()));
  }

  private static ChanceChoice sacrificeChoice() {
    return new ChanceChoice("sacrifice", "Sacrifice health.", new ChanceOutcome(-10, 25));
  }

  private static ChanceChoice leaveChoice() {
    return new ChanceChoice("leave", "Leave the shrine.", new ChanceOutcome(0, 0));
  }
}
