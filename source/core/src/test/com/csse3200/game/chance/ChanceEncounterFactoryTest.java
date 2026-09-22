package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.extensions.GameExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceEncounterFactoryTest {
  @Test
  void shouldCreateInitialEncountersInDeterministicOrder() {
    List<ChanceEncounter> encounters = ChanceEncounterFactory.createInitialEncounters();
    List<String> encounterIds = encounters.stream().map(ChanceEncounter::getId).toList();

    assertEquals(
        List.of(
            "mysterious-shrine",
            "wandering-healer",
            "flooded-crossing",
            "abandoned-mine",
            "wishing-fountain",
            "dice-game"),
        encounterIds);
    assertFalse(encounterIds.contains("forgotten-cache"));
    assertFalse(encounterIds.contains("roadside-riddle"));
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
  void shouldCreateWishingFountain() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(4);

    assertEquals("An old wishing fountain shimmers beside the path.", encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "make-wish", "Make a wish at the fountain.", 0, 0);
    assertChoice(encounter, 1, "leave", "Leave the fountain without making a wish.", 0, 0);
  }

  @Test
  void shouldCreateForcedCostFloodedCrossing() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(2);

    assertEquals("A flooded crossing blocks the road ahead.", encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "hire-ferryman", "Pay a ferryman for safe passage.", 0, -8);
    assertChoice(encounter, 1, "ford-river", "Attempt to ford the river alone.", -8, 0);
    assertNull(encounter.resolveChoice("wait"));
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
