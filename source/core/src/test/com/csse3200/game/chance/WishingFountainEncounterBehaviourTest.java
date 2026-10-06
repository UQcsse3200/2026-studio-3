package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.encounters.integration.CardServiceCatalogAdapter;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WishingFountainEncounterBehaviourTest {
  @Test
  void shouldLeaveWithoutRandomnessOrStateChangesAndCompleteNormally() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    int[] completionCount = {0};
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            1,
            wishingFountainEncounter(),
            new WishingFountainEncounterBehaviour(new FailingRandom(), cards("bandage")),
            new ChanceOutcomeApplier(player),
            (nodeId, success) -> completionCount[0]++);

    ChanceResolution result =
        session.resolveChoice(WishingFountainEncounterBehaviour.LEAVE_CHOICE_ID);

    assertTrue(result.isSuccess());
    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(70, player.getHealth());
    assertEquals(40, player.getCurrency());
    assertTrue(session.complete());
    assertEquals(1, completionCount[0]);
  }

  @Test
  void shouldApplyTwentyHealthThroughExistingOutcomeApplier() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(60, 100, 40);
    ChanceEncounterSession session =
        session(player, new SequenceRandom(0), cards("bandage"), new MockDeckGateway());

    ChanceResolution result =
        session.resolveChoice(WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID);

    assertTrue(result.isSuccess());
    assertEquals(20, result.getOutcome().getHealthDelta());
    assertEquals(80, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldUseExactProbabilityBoundaries() {
    assertEquals(20, wishOutcome(0).getHealthDelta());
    assertEquals(20, wishOutcome(49).getHealthDelta());

    assertEquals("bandage", wishOutcome(50, 0).getCardRewardId());
    assertEquals("bandage", wishOutcome(79, 0).getCardRewardId());

    assertTrue(wishOutcome(80).isNoEffect());
    assertTrue(wishOutcome(99).isNoEffect());
  }

  @Test
  void shouldAddDeterministicallySelectedRegisteredCardThroughExistingOutcomeApplier() {
    CardService cardService = cards("strike", "bandage");
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 50);
    MockDeckGateway deck = new MockDeckGateway();
    ChanceEncounterSession session = session(player, new SequenceRandom(50, 1), cardService, deck);

    ChanceResolution result =
        session.resolveChoice(WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID);

    assertTrue(result.isSuccess());
    assertEquals("strike", result.getOutcome().getCardRewardId());
    assertTrue(cardService.getCard(result.getOutcome().getCardRewardId()).isPresent());
    assertEquals(List.of("strike"), deck.getCardIds());
    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());
  }

  @Test
  void shouldApplyNoRewardOutcomeWithoutChangingState() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(70, 100, 40);
    ChanceEncounterSession session =
        session(player, new SequenceRandom(80), cards("bandage"), new MockDeckGateway());

    ChanceResolution result =
        session.resolveChoice(WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID);

    assertTrue(result.isSuccess());
    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(70, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldRejectUnknownChoice() {
    WishingFountainEncounterBehaviour behaviour =
        new WishingFountainEncounterBehaviour(new FailingRandom(), cards("bandage"));

    ChanceBehaviourResult result = behaviour.resolveChoice("missing");

    assertEquals(ChanceBehaviourResult.Type.INVALID_CHOICE, result.getType());
  }

  @Test
  void shouldRequireAtLeastOneEligibleCard() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WishingFountainEncounterBehaviour(new Random(266L), cards()));
  }

  private static ChanceEncounterSession session(
      MockPlayerStateGateway player, Random random, CardService cardService, MockDeckGateway deck) {
    return new ChanceEncounterSession(
        1,
        wishingFountainEncounter(),
        new WishingFountainEncounterBehaviour(random, cardService),
        new ChanceOutcomeApplier(player, new CardServiceCatalogAdapter(cardService), deck),
        (nodeId, success) -> {});
  }

  private static ChanceOutcome wishOutcome(int... randomValues) {
    ChanceBehaviourResult result =
        new WishingFountainEncounterBehaviour(new SequenceRandom(randomValues), cards("bandage"))
            .resolveChoice(WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID);
    return result.getOutcome();
  }

  private static ChanceEncounter wishingFountainEncounter() {
    return new ChanceEncounter(
        WishingFountainEncounterBehaviour.ENCOUNTER_ID,
        "An old wishing fountain shimmers beside the path.",
        List.of(
            new ChanceChoice(
                WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID,
                "Make a wish at the fountain.",
                new ChanceOutcome(0, 0)),
            new ChanceChoice(
                WishingFountainEncounterBehaviour.LEAVE_CHOICE_ID,
                "Leave the fountain without making a wish.",
                new ChanceOutcome(0, 0))),
        2);
  }

  private static CardService cards(String... cardIds) {
    return TestCardService.withCards(cardIds);
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

  private static final class FailingRandom extends Random {
    private static final long serialVersionUID = 1L;

    @Override
    public int nextInt(int bound) {
      throw new AssertionError("Leave and invalid choices must not consume randomness");
    }
  }
}
