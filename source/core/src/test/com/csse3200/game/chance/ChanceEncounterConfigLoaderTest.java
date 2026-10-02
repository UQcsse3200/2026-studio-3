package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.encounters.integration.CardServiceCatalogAdapter;
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
            "roadside-riddle",
            "corrupted-alchemist"),
        encounters.stream().map(ChanceEncounter::getId).toList());
    assertEquals(
        List.of(3, 2, 3, 2, 3, 2, 2, 2),
        encounters.stream().map(ChanceEncounter::getWeight).toList());
  }

  @Test
  void shouldPreserveConfiguredEncounterContent() {
    List<ChanceEncounter> encounters = ChanceEncounterConfigLoader.loadEncounters();

    assertEncounter(
        encounters.get(0),
        "mysterious-shrine",
        "A cracked sanctum shrine still burns with a god's spoiled light. An angel might leave an"
            + " offering here to learn what the fallen still demand.",
        new ExpectedChoice(
            "make-offering", "Bleed a little of your light into the shrine.", -10, 25),
        new ExpectedChoice("leave", "Pass without kneeling.", 0, 0));
    assertEncounter(
        encounters.get(1),
        "healing-spring",
        "Water wells from broken stone where a sanctum once washed the wounded. An angel could"
            + " drink and rise again for the climb ahead.",
        new ExpectedChoice("drink", "Drink from the spring.", 15, 0),
        new ExpectedChoice("leave", "Leave the water for whatever still wanders here.", 0, 0));
    assertEncounter(
        encounters.get(2),
        "forgotten-cache",
        "Under loose flagstones you find a sanctum cache left by servants who never returned. An"
            + " angel might claim what they meant for the climb.",
        new ExpectedChoice("take-coins", "Take the coins from the cache.", 0, 15),
        new ExpectedChoice(
            "claim-iron-oath", "Claim the iron oath tablet sealed inside.", 0, 0, "iron_oath"),
        new ExpectedChoice("leave", "Leave the cache buried.", 0, 0));
    assertEncounter(
        encounters.get(3),
        "wandering-healer",
        "A faded attendant still tends the hurt along the sanctum road, following orders that once"
            + " meant mercy. An angel could accept that help without asking who gives it.",
        new ExpectedChoice("purchase-remedy", "Buy the attendant's restorative draught.", 20, -10),
        new ExpectedChoice(
            "accept-bandage", "Accept a spare bandage for the road.", 0, 0, "bandage"),
        new ExpectedChoice("decline", "Decline and continue the climb.", 0, 0));
    assertEncounter(
        encounters.get(4),
        "flooded-crossing",
        "A flooded sanctum court bars the way upward. An angel must choose how to cross without"
            + " abandoning the path.",
        new ExpectedChoice("hire-ferryman", "Pay a silent ferryman for safe passage.", 0, -8),
        new ExpectedChoice("ford-river", "Wade the flood alone.", -8, 0),
        new ExpectedChoice("wait", "Wait for the waters to sink.", 0, 0));
    assertEncounter(
        encounters.get(5),
        "abandoned-mine",
        "Beneath the sanctum, old workings still hold relics of the fallen. An angel might risk the"
            + " dark for tools of release.",
        new ExpectedChoice("search-tunnels", "Search the unstable tunnels for valuables.", -12, 30),
        new ExpectedChoice(
            "recover-doom-sigil", "Pull a doom sigil free from the wall.", -5, 0, "doom_sigil"),
        new ExpectedChoice("leave", "Leave the workings closed.", 0, 0));
    assertEncounter(
        encounters.get(6),
        "roadside-riddle",
        "A hooded servant still tests travellers with an old sanctum riddle. An angel who answers"
            + " may earn coin—or a sealed rite instead.",
        new ExpectedChoice("answer-riddle", "Answer the riddle for coin.", 0, 12),
        new ExpectedChoice(
            "accept-sealed-pact", "Accept a sealed pact in place of coin.", 0, 0, "sealed_pact"),
        new ExpectedChoice("walk-on", "Walk on without answering.", 0, 0));
    assertEncounter(
        encounters.get(7),
        "corrupted-alchemist",
        "A sanctum alchemist still mixes reagents from the corruption itself. An angel might trade"
            + " for poison—or take a purifying draught.",
        new ExpectedChoice(
            "buy-poison-flask", "Trade coins for a poison flask.", 0, -12, "poison_flask"),
        new ExpectedChoice("take-purify", "Accept a purifying tincture for free.", 0, 0, "purify"),
        new ExpectedChoice("refuse", "Refuse the bargains.", 0, 0));
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
  void shouldRejectBlankAndNonStringCardRewards() {
    ChanceEncounterLoadingException exception = loadInvalid("invalid_card_rewards.json");

    assertMessageContains(
        exception,
        "choice[0].outcome.cardRewardId must not be blank when present",
        "choice[1].outcome.cardRewardId must be a string when present",
        "choice[2].outcome.cardRewardId must not be null when present");
  }

  @Test
  void shouldRejectUnknownCardRewardThroughCatalogBoundary() {
    ChanceEncounterLoadingException exception =
        assertThrows(
            ChanceEncounterLoadingException.class,
            () ->
                ChanceEncounterConfigLoader.loadEncounters(
                    TEST_DIRECTORY + "unknown_card_reward.json",
                    new CardServiceCatalogAdapter(new CardLibrary(CardConfigLoader.loadCards()))));

    assertMessageContains(exception, "unknown cardRewardId 'missing-card'");
  }

  @Test
  void shouldLoadConfiguredRewardThroughProductionCardServiceAdapter() {
    List<ChanceEncounter> encounters =
        ChanceEncounterConfigLoader.loadEncountersWithCatalog(
            new CardServiceCatalogAdapter(new CardLibrary(CardConfigLoader.loadCards())));

    ChanceOutcome reward = encounters.get(3).resolveChoice("accept-bandage");

    assertEquals("bandage", reward.getCardRewardId());
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
        new ChanceEncounterSelector(encounters, new SequenceRandom(0, 3, 5, 8, 10, 13, 15, 17));

    List<String> selectedIds =
        List.of(
            selector.select().getId(),
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
      assertEquals(expected.cardRewardId(), outcome.getCardRewardId());
      assertSame(outcome, encounter.resolveChoice(expected.id()));
    }
  }

  private record ExpectedChoice(
      String id, String description, int healthDelta, int currencyDelta, String cardRewardId) {
    private ExpectedChoice(String id, String description, int healthDelta, int currencyDelta) {
      this(id, description, healthDelta, currencyDelta, null);
    }
  }

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
