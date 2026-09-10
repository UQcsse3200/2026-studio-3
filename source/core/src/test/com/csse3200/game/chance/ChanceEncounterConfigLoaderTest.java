package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceEncounterConfigLoaderTest {
  private static final String TEST_DIRECTORY = "test/chance/";

  @Test
  void shouldLoadAllConfiguredEncountersFromDefaultFile() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();

    assertEquals(
        List.of(
            "mysterious-shrine",
            "healing-spring",
            "forgotten-cache",
            "wandering-healer",
            "flooded-crossing",
            "abandoned-mine",
            "roadside-riddle"),
        encounters.stream().map(ChanceEncounter::getId).toList());
    assertEquals(
        List.of(3, 2, 3, 2, 3, 2, 2), encounters.stream().map(ChanceEncounter::getWeight).toList());
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
    assertEncounter(
        encounters.get(3),
        "wandering-healer",
        "A wandering healer offers a restorative draught for a modest fee.",
        new ExpectedChoice("purchase-remedy", "Buy the healer's restorative draught.", 20, -10),
        new ExpectedChoice("decline", "Politely decline the healer's offer.", 0, 0));
    assertEncounter(
        encounters.get(4),
        "flooded-crossing",
        "A flooded crossing blocks the road ahead.",
        new ExpectedChoice("hire-ferryman", "Pay a ferryman for safe passage.", 0, -8),
        new ExpectedChoice("ford-river", "Attempt to ford the river alone.", -8, 0),
        new ExpectedChoice("wait", "Wait for the water to recede.", 0, 0));
    assertEncounter(
        encounters.get(5),
        "abandoned-mine",
        "The mouth of an abandoned mine promises danger and forgotten riches.",
        new ExpectedChoice("search-tunnels", "Search the unstable tunnels for valuables.", -12, 30),
        new ExpectedChoice("leave", "Leave the mine undisturbed.", 0, 0));
    assertEncounter(
        encounters.get(6),
        "roadside-riddle",
        "A hooded traveller offers a coin reward for solving a riddle.",
        new ExpectedChoice("answer-riddle", "Attempt to solve the traveller's riddle.", 0, 12),
        new ExpectedChoice("walk-on", "Continue along the road.", 0, 0));
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

  @Test
  void shouldRejectInvalidEncounterIdsAndDescriptions() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_encounter_fields.json");

    assertMessageContains(
        exception,
        "encounter[0]: id must not be null or blank",
        "encounter[1]: id must not be null or blank",
        "encounter[2]: description must not be null or blank",
        "encounter[3]: description must not be null or blank");
  }

  @Test
  void shouldRejectDuplicateEncounterIds() {
    ChanceEncounterLoadingException exception = loadInvalid("duplicate_encounter_ids.json");

    assertMessageContains(exception, "duplicate encounter ID 'duplicate'");
  }

  @Test
  void shouldRejectEncounterWithoutChoices() {
    ChanceEncounterLoadingException exception = loadInvalid("no_choices.json");

    assertMessageContains(exception, "encounter[0]: must define at least one choice");
  }

  @Test
  void shouldRejectInvalidChoiceIdsAndDescriptions() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_choice_fields.json");

    assertMessageContains(
        exception,
        "choice[0]: id must not be null or blank",
        "choice[1]: id must not be null or blank",
        "choice[2]: description must not be null or blank",
        "choice[3]: description must not be null or blank");
  }

  @Test
  void shouldRejectDuplicateChoiceIdsWithinEncounter() {
    ChanceEncounterLoadingException exception = loadInvalid("duplicate_choice_ids.json");

    assertMessageContains(exception, "duplicate choice ID 'duplicate'");
  }

  @Test
  void shouldRejectMissingNullAndIncompleteOutcomes() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_outcomes.json");

    assertMessageContains(
        exception,
        "choice[0]: outcome must not be null",
        "choice[1]: outcome must not be null",
        "choice[2].outcome: currencyDelta must be present",
        "choice[3].outcome: healthDelta must be present");
  }

  @Test
  void shouldRejectStructurallyInvalidOutcome() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_outcome_structure.json");

    assertMessageContains(exception, "choice[0]: outcome must be a JSON object");
  }

  @Test
  void shouldRejectUnsafeNumericDeltas() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_numeric_outcomes.json");

    assertMessageContains(
        exception,
        "choice[0].outcome.healthDelta must be an integer",
        "choice[1].outcome.healthDelta must be an exact 32-bit integer",
        "choice[1].outcome.currencyDelta must be an exact 32-bit integer",
        "choice[2].outcome.healthDelta must be an exact 32-bit integer");
  }

  @Test
  void shouldAcceptNoEffectAndIntegerBoundaryDeltas() {
    List<ChanceEncounter> encounters =
        ChanceEncounterConfigLoader.loadEncounters(
            TEST_DIRECTORY + "valid_outcome_boundaries.json");

    ChanceEncounter encounter = encounters.get(0);
    ChanceOutcome noEffect = encounter.resolveChoice("no-effect");
    ChanceOutcome boundaries = encounter.resolveChoice("integer-boundaries");

    assertTrue(noEffect.isNoEffect());
    assertEquals(Integer.MIN_VALUE, boundaries.getHealthDelta());
    assertEquals(Integer.MAX_VALUE, boundaries.getCurrencyDelta());
  }

  @Test
  void shouldRejectMissingZeroAndNegativeWeights() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_weights.json");

    assertMessageContains(
        exception,
        "encounter[0]: weight must be positive, was 0",
        "encounter[1]: weight must be positive, was -1",
        "encounter[2]: weight must be present");
  }

  @Test
  void shouldRejectUnsafeNumericWeights() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_numeric_weights.json");

    assertMessageContains(
        exception,
        "encounter[0].weight must be an integer",
        "encounter[1].weight must be an exact 32-bit integer");
  }

  @Test
  void shouldRejectSelectionPoolTotalWeightOverflow() {
    ChanceEncounterLoadingException exception = loadInvalid("total_weight_overflow.json");

    assertMessageContains(
        exception, "selection pool total weight must not exceed " + Integer.MAX_VALUE);
  }

  @Test
  void shouldSelectEveryEncounterFromExpandedConfiguration() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();
    ChanceEncounterSelector selector =
        new ChanceEncounterSelector(encounters, new SequenceRandom(0, 3, 5, 8, 10, 13, 15));

    List<String> selectedIds =
        List.of(
            selector.select().getId(),
            selector.select().getId(),
            selector.select().getId(),
            selector.select().getId(),
            selector.select().getId(),
            selector.select().getId(),
            selector.select().getId());

    assertEquals(encounters.stream().map(ChanceEncounter::getId).toList(), selectedIds);
    assertEquals(encounters.size(), Set.copyOf(selectedIds).size());
  }

  private static ChanceEncounterLoadingException loadInvalid(String filename) {
    return assertThrows(
        ChanceEncounterLoadingException.class,
        () -> ChanceEncounterConfigLoader.loadEncounters(TEST_DIRECTORY + filename));
  }

  private static void assertMessageContains(
      ChanceEncounterLoadingException exception, String... expectedFragments) {
    for (String expectedFragment : expectedFragments) {
      assertTrue(exception.getMessage().contains(expectedFragment), exception.getMessage());
    }
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

  private static final class SequenceRandom extends Random {
    private static final long serialVersionUID = 1L;

    private final int[] values;
    private int index;

    private SequenceRandom(int... values) {
      this.values = values.clone();
    }

    @Override
    public int nextInt(int bound) {
      int value = values[index++];
      if (value < 0 || value >= bound) {
        throw new IllegalArgumentException("Test value is outside random bound " + bound);
      }
      return value;
    }
  }
}
