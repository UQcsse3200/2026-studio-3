package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    assertEquals(
        List.of(
            "mysterious-shrine",
            "healing-spring",
            "forgotten-cache",
            "wandering-healer",
            "flooded-crossing",
            "abandoned-mine",
            "roadside-riddle",
            "corrupted-alchemist"),
        encounters.stream().map(ChanceEncounter::getId).toList());
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

    assertEquals(
        "A cracked sanctum shrine still burns with a god's spoiled light. An angel might leave an"
            + " offering here to learn what the fallen still demand.",
        encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(
        encounter, 0, "make-offering", "Bleed a little of your light into the shrine.", -10, 25);
    assertChoice(encounter, 1, "leave", "Pass without kneeling.", 0, 0);
  }

  @Test
  void shouldCreateHealingSpring() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(1);

    assertEquals(
        "Water wells from broken stone where a sanctum once washed the wounded. An angel could"
            + " drink and rise again for the climb ahead.",
        encounter.getDescription());
    assertEquals(2, encounter.getChoices().size());
    assertChoice(encounter, 0, "drink", "Drink from the spring.", 15, 0);
    assertChoice(encounter, 1, "leave", "Leave the water for whatever still wanders here.", 0, 0);
  }

  @Test
  void shouldCreateForgottenCache() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(2);

    assertEquals(
        "Under loose flagstones you find a sanctum cache left by servants who never returned. An"
            + " angel might claim what they meant for the climb.",
        encounter.getDescription());
    assertEquals(3, encounter.getChoices().size());
    assertChoice(encounter, 0, "take-coins", "Take the coins from the cache.", 0, 15);
    assertChoice(
        encounter, 1, "claim-iron-oath", "Claim the iron oath tablet sealed inside.", 0, 0);
    assertChoice(encounter, 2, "leave", "Leave the cache buried.", 0, 0);
  }

  @Test
  void shouldCreateCorruptedAlchemist() {
    ChanceEncounter encounter = ChanceEncounterFactory.createInitialEncounters().get(7);

    assertEquals(
        "A sanctum alchemist still mixes reagents from the corruption itself. An angel might trade"
            + " for poison—or take a purifying draught.",
        encounter.getDescription());
    assertEquals(3, encounter.getChoices().size());
    assertChoice(encounter, 0, "buy-poison-flask", "Trade coins for a poison flask.", 0, -12);
    assertChoice(encounter, 1, "take-purify", "Accept a purifying tincture for free.", 0, 0);
    assertChoice(encounter, 2, "refuse", "Refuse the bargains.", 0, 0);
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
    assertEquals(
        expectedHealthDelta == 0 && expectedCurrencyDelta == 0 && outcome.getCardRewardId() == null,
        outcome.isNoEffect());
    assertSame(outcome, encounter.resolveChoice(expectedId));
  }
}
