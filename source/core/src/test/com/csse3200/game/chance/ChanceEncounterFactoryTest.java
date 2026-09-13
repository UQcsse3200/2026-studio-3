package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChanceEncounterFactoryTest {
  @Test
  void shouldCreateInitialEncountersInDeterministicOrder() {
    List<ChanceEncounter> encounters = ChanceEncounterFactory.createInitialEncounters();

    assertEquals(3, encounters.size());
    assertEquals("mysterious-shrine", encounters.get(0).getId());
    assertEquals("healing-spring", encounters.get(1).getId());
    assertEquals("forgotten-cache", encounters.get(2).getId());
  }

  @Test
  void shouldCreateUniqueEncounterIds() {
    List<ChanceEncounter> encounters = ChanceEncounterFactory.createInitialEncounters();
    Set<String> encounterIds = new HashSet<>();

    for (ChanceEncounter encounter : encounters) {
      encounterIds.add(encounter.getId());
    }

    assertEquals(encounters.size(), encounterIds.size());
  }

  @Test
  void shouldExposeReadOnlyInitialEncounters() {
    List<ChanceEncounter> encounters = ChanceEncounterFactory.createInitialEncounters();

    assertThrows(UnsupportedOperationException.class, encounters::clear);
  }

  @Test
  void shouldCreateMysteriousShrine() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(0);

    assertEquals("An ancient shrine hums with an unsettling energy.", encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "make-offering", "Offer some of your vitality.", -10, 25);
    assertChoice(encounter, 1, "leave", "Leave the shrine untouched.", 0, 0);
  }

  @Test
  void shouldCreateHealingSpring() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(1);

    assertEquals("A clear spring glows softly beside the path.", encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "drink", "Drink from the spring.", 15, 0);
    assertChoice(encounter, 1, "leave", "Continue without drinking.", 0, 0);
  }

  @Test
  void shouldCreateForgottenCache() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(2);

    assertEquals(
        "You discover an abandoned cache hidden beneath loose stones.", encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "take-coins", "Take the coins from the cache.", 0, 15);
    assertChoice(encounter, 1, "leave", "Leave the cache untouched.", 0, 0);
  }

  private static void assertChoice(
      ChanceEncounter encounter,
      int index,
      String expectedId,
      String expectedDescription,
      int expectedHealthDelta,
      int expectedCurrencyDelta) {
    ChanceChoice choice = encounter.getChoices().get(index);
    ChanceOutcome outcome = choice.resolve();

    assertEquals(expectedId, choice.getId());
    assertEquals(expectedDescription, choice.getDescription());
    assertEquals(expectedHealthDelta, outcome.getHealthDelta());
    assertEquals(expectedCurrencyDelta, outcome.getCurrencyDelta());
    assertEquals(expectedHealthDelta == 0 && expectedCurrencyDelta == 0, outcome.isNoEffect());
    assertSame(outcome, encounter.resolveChoice(expectedId));
  }
}
