package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.encounters.integration.CardServiceCatalogAdapter;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceOutcomeApplier;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.encounters.integration.mocks.MockDeckGateway;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DiceEncounterBehaviourTest {
  @Test
  void shouldExposeActualDiceFacesWithoutRollingAgainForStageChoices() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 2, 3);

    assertTrue(behaviour.getLastDiceRoll().isEmpty());
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    assertEquals(new DiceRoll(1, 6, 1), behaviour.getLastDiceRoll().orElseThrow());

    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    assertEquals(new DiceRoll(1, 6, 1), behaviour.getLastDiceRoll().orElseThrow());

    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);
    assertEquals(new DiceRoll(3, 4, 2), behaviour.getLastDiceRoll().orElseThrow());
  }

  @Test
  void shouldExposeOnlyChoicesForCurrentDiceStage() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 0, 0, 0, 5, 0);
    ChanceEncounter encounter = diceEncounter();

    assertEquals(List.of("low", "high"), behaviour.getAvailableChoiceIds(encounter));
    assertEquals("ROUND 1 - PREDICT LOW OR HIGH", behaviour.getChoicePrompt());

    behaviour.resolveChoice("low");
    assertEquals(List.of("take", "double-down"), behaviour.getAvailableChoiceIds(encounter));

    behaviour.resolveChoice("double-down");
    assertEquals(List.of("low", "high"), behaviour.getAvailableChoiceIds(encounter));

    behaviour.resolveChoice("low");
    assertEquals(List.of("cash-out", "continue"), behaviour.getAvailableChoiceIds(encounter));
    assertTrue(behaviour.getChoicePrompt().contains("30 GOLD"));

    behaviour.resolveChoice("continue");
    assertEquals(List.of("low", "high"), behaviour.getAvailableChoiceIds(encounter));
    assertEquals("ROUND 2 - PREDICT LOW OR HIGH", behaviour.getChoicePrompt());

    behaviour.resolveChoice("low");
    assertEquals(List.of("take", "double-down"), behaviour.getAvailableChoiceIds(encounter));

    behaviour.resolveChoice("take");
    assertEquals(List.of(), behaviour.getAvailableChoiceIds(encounter));
  }

  @Test
  void shouldWinLowAndHighPredictionsAcrossTheirInclusiveRanges() {
    assertStakeAfterPrediction(DiceEncounterBehaviour.LOW_CHOICE_ID, 2, 0, 0);
    assertStakeAfterPrediction(DiceEncounterBehaviour.LOW_CHOICE_ID, 6, 2, 2);
    assertStakeAfterPrediction(DiceEncounterBehaviour.HIGH_CHOICE_ID, 8, 3, 3);
    assertStakeAfterPrediction(DiceEncounterBehaviour.HIGH_CHOICE_ID, 12, 5, 5);
  }

  @Test
  void shouldEndSuccessfullyWithNoRewardForIncorrectPrediction() {
    DiceEncounterBehaviour lowPrediction = behaviour(3, 3);
    DiceEncounterBehaviour highPrediction = behaviour(2, 2);

    ChanceBehaviourResult lowLoss =
        lowPrediction.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    ChanceBehaviourResult highLoss =
        highPrediction.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.OUTCOME, lowLoss.getType());
    assertTrue(lowLoss.getOutcome().isNoEffect());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, lowPrediction.getStage());
    assertTrue(highLoss.getOutcome().isNoEffect());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, highPrediction.getStage());
  }

  @Test
  void shouldEnterLuckySevenInsteadOfResolvingPrediction() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_ONE_LUCKY_SEVEN, behaviour.getStage());
    assertEquals(0, behaviour.getCurrentStake());
  }

  @Test
  void shouldCreateTenGoldStakeWhenTakingRoundOneLuckySeven() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5);
    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(10, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());
  }

  @Test
  void shouldEnterFinalRoundOneLuckySevenAndTakeThirtyGoldStake() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 2, 3);
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);

    ChanceBehaviourResult secondSeven =
        behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, secondSeven.getType());
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_ONE_FINAL_LUCKY_SEVEN, behaviour.getStage());

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(30, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());
  }

  @Test
  void shouldCreateSixtyGoldStakeForFinalRoundOneWin() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 2, 3, 5, 5);
    advanceToRoundOneFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(60, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());
  }

  @Test
  void shouldTreatFinalRoundOneSevenAsSixtyGoldJackpotWithoutFurtherNesting() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 2, 3, 0, 5);
    advanceToRoundOneFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(60, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());
  }

  @Test
  void shouldEndWithoutRewardForFinalRoundOneLoss() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 2, 3, 0, 0);
    advanceToRoundOneFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldEndWithNoRewardForRoundOneDoubleDownLoss() {
    DiceEncounterBehaviour behaviour = behaviour(0, 5, 0, 0);
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldConvergeEveryPositiveRoundOnePathAndCashOutExactStake() {
    DiceEncounterBehaviour tenGold = behaviour(0, 5);
    tenGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    tenGold.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    DiceEncounterBehaviour fifteenGold = behaviour(0, 0);
    fifteenGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);

    DiceEncounterBehaviour thirtyGold = behaviour(0, 5, 5, 5);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    DiceEncounterBehaviour sixtyGold = behaviour(0, 5, 2, 3, 5, 5);
    advanceToRoundOneFinalPrediction(sixtyGold);
    sixtyGold.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertCashOut(tenGold, 10);
    assertCashOut(fifteenGold, 15);
    assertCashOut(thirtyGold, 30);
    assertCashOut(sixtyGold, 60);
  }

  @Test
  void shouldUseOneRoundTwoPathForTenFifteenThirtyAndSixtyGoldStakes() {
    DiceEncounterBehaviour tenGold = behaviour(0, 5);
    tenGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    tenGold.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    DiceEncounterBehaviour fifteenGold = behaviour(0, 0);
    fifteenGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);

    DiceEncounterBehaviour thirtyGold = behaviour(0, 5, 5, 5);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    thirtyGold.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    DiceEncounterBehaviour sixtyGold = behaviour(0, 5, 2, 3, 5, 5);
    advanceToRoundOneFinalPrediction(sixtyGold);
    sixtyGold.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    for (DiceEncounterBehaviour candidate : List.of(tenGold, fifteenGold, thirtyGold, sixtyGold)) {
      ChanceBehaviourResult result =
          candidate.resolveChoice(DiceEncounterBehaviour.CONTINUE_CHOICE_ID);
      assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
      assertEquals(DiceEncounterBehaviour.Stage.ROUND_TWO_PREDICTION, candidate.getStage());
      assertEquals(DiceEncounterBehaviour.Round.TWO, candidate.getCurrentRound());
    }
  }

  @Test
  void shouldPreserveStakeAndAwardOneEligibleCardForRoundTwoNormalWin() {
    DiceEncounterBehaviour behaviour =
        behaviour(TestCardService.withCards("strike", "bandage"), 0, 0, 5, 5, 1);
    advanceToRoundTwo(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("strike"), result.getOutcome().getCardRewardIds());
  }

  @Test
  void shouldLoseEntireStakeAndAwardNoCardForRoundTwoNormalLoss() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 0);
    advanceToRoundTwo(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(0, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldTakeRoundTwoLuckySevenForStakeAndOneCard() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 0);
    advanceToRoundTwo(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("bandage"), result.getOutcome().getCardRewardIds());
  }

  @Test
  void shouldWinTwoDuplicateCardsForFirstRoundTwoDoubleDown() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 5, 5, 0, 0);
    advanceToRoundTwo(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("bandage", "bandage"), result.getOutcome().getCardRewardIds());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldEnterFinalRoundTwoLuckySevenAndTakeTwoCards() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 2, 3, 0, 0);
    advanceToRoundTwoFinalLuckySeven(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("bandage", "bandage"), result.getOutcome().getCardRewardIds());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldAwardThreeCardsForFinalRoundTwoWin() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 2, 3, 5, 5, 0, 0, 0);
    advanceToRoundTwoFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("bandage", "bandage", "bandage"), result.getOutcome().getCardRewardIds());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldTreatFinalRoundTwoSevenAsThreeCardJackpotWithoutFurtherNesting() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 2, 3, 0, 5, 0, 0, 0);
    advanceToRoundTwoFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertEquals(15, result.getOutcome().getCurrencyDelta());
    assertEquals(List.of("bandage", "bandage", "bandage"), result.getOutcome().getCardRewardIds());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
    assertEquals(
        ChanceBehaviourResult.Type.INVALID_CHOICE,
        behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID).getType());
  }

  @Test
  void shouldLoseEverythingForFinalRoundTwoLoss() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 2, 3, 0, 0);
    advanceToRoundTwoFinalPrediction(behaviour);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(result.getOutcome().isNoEffect());
    assertEquals(0, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.RESOLVED, behaviour.getStage());
  }

  @Test
  void shouldLoseStakeAndCardsForRoundTwoDoubleDownLoss() {
    DiceEncounterBehaviour behaviour = behaviour(0, 0, 0, 5, 0, 0);
    advanceToRoundTwo(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);

    ChanceBehaviourResult result = behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(result.getOutcome().isNoEffect());
  }

  @Test
  void shouldFilterDeduplicateAndSortEligibleCardIds() {
    CardConfig zeta = card("zeta");
    CardConfig blank = card(" ");
    CardConfig alpha = card("alpha");
    CardService cards = cardService(Arrays.asList(zeta, null, blank, alpha, zeta));
    DiceEncounterBehaviour behaviour = behaviour(cards, 0, 0, 0, 5, 1);
    advanceToRoundTwo(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);

    ChanceOutcome outcome =
        behaviour.resolveChoice(DiceEncounterBehaviour.TAKE_CHOICE_ID).getOutcome();

    assertEquals(List.of("zeta"), outcome.getCardRewardIds());
  }

  @Test
  void shouldRejectEmptyEligibleCardPool() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DiceEncounterBehaviour(new Random(266L), TestCardService.withCards()));
  }

  @Test
  void shouldApplyNothingBeforeFinalResolutionAndCompleteExactlyOnce() {
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    MockDeckGateway deck = new MockDeckGateway();
    int[] completionCount = {0};
    CardService cards = TestCardService.withCards("bandage");
    DiceEncounterBehaviour behaviour = behaviour(cards, 0, 0, 5, 5, 0);
    ChanceEncounterSession session =
        new ChanceEncounterSession(
            8,
            diceEncounter(),
            behaviour,
            new ChanceOutcomeApplier(player, new CardServiceCatalogAdapter(cards), deck),
            (nodeId, success) -> completionCount[0]++);

    assertEquals(
        List.of("low", "high"),
        session.getAvailableChoices().stream().map(ChanceChoice::getId).toList());
    ChanceResolution roundOne = session.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    assertEquals(
        List.of("cash-out", "continue"),
        session.getAvailableChoices().stream().map(ChanceChoice::getId).toList());
    ChanceResolution continueResult =
        session.resolveChoice(DiceEncounterBehaviour.CONTINUE_CHOICE_ID);
    assertEquals(
        List.of("low", "high"),
        session.getAvailableChoices().stream().map(ChanceChoice::getId).toList());

    assertEquals(ChanceResolution.Status.AWAITING_CHOICE, roundOne.getStatus());
    assertEquals(ChanceResolution.Status.AWAITING_CHOICE, continueResult.getStatus());
    assertEquals(40, player.getCurrency());
    assertTrue(deck.getCardIds().isEmpty());
    assertFalse(session.complete());

    ChanceResolution finalResult = session.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);

    assertTrue(finalResult.isSuccess());
    assertEquals(55, player.getCurrency());
    assertEquals(List.of("bandage"), deck.getCardIds());
    assertTrue(session.complete());
    assertFalse(session.complete());
    assertEquals(1, completionCount[0]);
  }

  private static void assertStakeAfterPrediction(
      String choiceId, int expectedTotal, int firstDie, int secondDie) {
    DiceEncounterBehaviour behaviour = behaviour(firstDie, secondDie);

    ChanceBehaviourResult result = behaviour.resolveChoice(choiceId);

    assertEquals(expectedTotal, firstDie + secondDie + 2);
    assertEquals(ChanceBehaviourResult.Type.AWAITING_CHOICE, result.getType());
    assertEquals(15, behaviour.getCurrentStake());
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());
  }

  private static void assertCashOut(DiceEncounterBehaviour behaviour, int expectedStake) {
    assertEquals(DiceEncounterBehaviour.Stage.STAKE_DECISION, behaviour.getStage());

    ChanceBehaviourResult result =
        behaviour.resolveChoice(DiceEncounterBehaviour.CASH_OUT_CHOICE_ID);

    assertEquals(expectedStake, result.getOutcome().getCurrencyDelta());
    assertTrue(result.getOutcome().getCardRewardIds().isEmpty());
  }

  private static void advanceToRoundTwo(DiceEncounterBehaviour behaviour) {
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.CONTINUE_CHOICE_ID);
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_TWO_PREDICTION, behaviour.getStage());
  }

  private static void advanceToRoundOneFinalPrediction(DiceEncounterBehaviour behaviour) {
    behaviour.resolveChoice(DiceEncounterBehaviour.LOW_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_ONE_FINAL_LUCKY_SEVEN, behaviour.getStage());
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_ONE_FINAL_PREDICTION, behaviour.getStage());
  }

  private static void advanceToRoundTwoFinalLuckySeven(DiceEncounterBehaviour behaviour) {
    advanceToRoundTwo(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    behaviour.resolveChoice(DiceEncounterBehaviour.HIGH_CHOICE_ID);
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_TWO_FINAL_LUCKY_SEVEN, behaviour.getStage());
  }

  private static void advanceToRoundTwoFinalPrediction(DiceEncounterBehaviour behaviour) {
    advanceToRoundTwoFinalLuckySeven(behaviour);
    behaviour.resolveChoice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID);
    assertEquals(DiceEncounterBehaviour.Stage.ROUND_TWO_FINAL_PREDICTION, behaviour.getStage());
  }

  private static DiceEncounterBehaviour behaviour(int... randomValues) {
    return behaviour(TestCardService.withCards("bandage"), randomValues);
  }

  private static DiceEncounterBehaviour behaviour(CardService cards, int... randomValues) {
    return new DiceEncounterBehaviour(new SequenceRandom(randomValues), cards);
  }

  private static ChanceEncounter diceEncounter() {
    return new ChanceEncounter(
        DiceEncounterBehaviour.ENCOUNTER_ID,
        "A dice keeper offers a wager.",
        List.of(
            choice(DiceEncounterBehaviour.LOW_CHOICE_ID),
            choice(DiceEncounterBehaviour.HIGH_CHOICE_ID),
            choice(DiceEncounterBehaviour.TAKE_CHOICE_ID),
            choice(DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID),
            choice(DiceEncounterBehaviour.CASH_OUT_CHOICE_ID),
            choice(DiceEncounterBehaviour.CONTINUE_CHOICE_ID)));
  }

  private static ChanceChoice choice(String id) {
    return new ChanceChoice(id, id, new ChanceOutcome(0, 0));
  }

  private static CardConfig card(String id) {
    CardConfig card = new CardConfig();
    card.id = id;
    return card;
  }

  private static CardService cardService(List<CardConfig> cards) {
    return new CardService() {
      @Override
      public Optional<CardConfig> getCard(String cardId) {
        return cards.stream().filter(card -> card != null && card.id.equals(cardId)).findFirst();
      }

      @Override
      public List<CardConfig> getAllCards() {
        return cards;
      }
    };
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
      if (index >= values.length) {
        throw new AssertionError("Deterministic random sequence exhausted");
      }
      int value = values[index++];
      if (value < 0 || value >= bound) {
        throw new AssertionError("Test value is outside random bound " + bound);
      }
      return value;
    }
  }
}
