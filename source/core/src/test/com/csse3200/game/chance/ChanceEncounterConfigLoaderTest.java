package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceEncounterConfigLoaderTest {
  private static final String TEST_DIRECTORY = "test/chance/";

  @Test
  void shouldLoadAllInitialEncountersFromDefaultFile() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();

    assertEquals(3, encounters.size());
    assertEquals("mysterious-shrine", encounters.get(0).getId());
    assertEquals("healing-spring", encounters.get(1).getId());
    assertEquals("forgotten-cache", encounters.get(2).getId());
  }

  @Test
  void shouldPreserveConfiguredEncounterContent() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();

    assertEncounter(
        encounters.get(0),
        "mysterious-shrine",
        "An ancient shrine hums with an unsettling energy.",
        new ExpectedChoice("make-offering", "Offer some of your vitality.", -10, 25),
        new ExpectedChoice("leave", "Leave the shrine untouched.", 0, 0));
    assertEncounter(
        encounters.get(1),
        "healing-spring",
        "A clear spring glows softly beside the path.",
        new ExpectedChoice("drink", "Drink from the spring.", 15, 0),
        new ExpectedChoice("leave", "Continue without drinking.", 0, 0));
    assertEncounter(
        encounters.get(2),
        "forgotten-cache",
        "You discover an abandoned cache hidden beneath loose stones.",
        new ExpectedChoice("take-coins", "Take the coins from the cache.", 0, 15),
        new ExpectedChoice("leave", "Leave the cache untouched.", 0, 0));
  }

  @Test
  void shouldReturnImmutableDomainCollections() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();

    assertThrows(UnsupportedOperationException.class, encounters::clear);
    assertThrows(UnsupportedOperationException.class, encounters.get(0).getChoices()::clear);
  }

  @Test
  void shouldRejectMalformedJson() {
    ChanceEncounterLoadingException exception =
        assertThrows(
            ChanceEncounterLoadingException.class,
            () -> ChanceEncounterConfigLoader.loadEncounters(TEST_DIRECTORY + "malformed.json"));

    assertTrue(exception.getMessage().contains("Malformed"));
  }

  private static void assertEncounter(
      ChanceEncounter encounter,
      String expectedId,
      String expectedDescription,
      ExpectedChoice... expectedChoices) {
    assertEquals(expectedId, encounter.getId());
    assertEquals(expectedDescription, encounter.getDescription());
    assertEquals(expectedChoices.length, encounter.getChoices().size());

    for (int i = 0; i < expectedChoices.length; i++) {
      ExpectedChoice expected = expectedChoices[i];
      ChanceChoice choice = encounter.getChoices().get(i);
      ChanceOutcome outcome = choice.resolve();

      assertEquals(expected.id(), choice.getId());
      assertEquals(expected.description(), choice.getDescription());
      assertEquals(expected.healthDelta(), outcome.getHealthDelta());
      assertEquals(expected.currencyDelta(), outcome.getCurrencyDelta());
      assertSame(outcome, encounter.resolveChoice(expected.id()));
    }
  }

  private record ExpectedChoice(
      String id, String description, int healthDelta, int currencyDelta) {}
}
