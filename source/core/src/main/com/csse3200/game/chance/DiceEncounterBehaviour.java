package com.csse3200.game.chance;

import com.csse3200.game.cards.CardService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/** Resolves the staged, two-round push-your-luck Dice Event. */
public final class DiceEncounterBehaviour implements ChanceEncounterBehaviour {
  /** Stable identifier of the Dice Event catalogue definition. */
  public static final String ENCOUNTER_ID = "dice-game";

  /** Stable identifier for predicting a total from 2 through 6. */
  public static final String LOW_CHOICE_ID = "low";

  /** Stable identifier for predicting a total from 8 through 12. */
  public static final String HIGH_CHOICE_ID = "high";

  /** Stable identifier for accepting a Lucky Seven offer. */
  public static final String TAKE_CHOICE_ID = "take";

  /** Stable identifier for risking a Lucky Seven offer on another prediction. */
  public static final String DOUBLE_DOWN_CHOICE_ID = "double-down";

  /** Stable identifier for accepting the current Gold stake. */
  public static final String CASH_OUT_CHOICE_ID = "cash-out";

  /** Stable identifier for risking the current Gold stake in round two. */
  public static final String CONTINUE_CHOICE_ID = "continue";

  private static final int DIE_SIDES = 6;
  private static final int LUCKY_SEVEN = 7;
  private static final int ROUND_ONE_NORMAL_STAKE = 15;
  private static final int ROUND_ONE_LUCKY_TAKE_STAKE = 10;
  private static final int ROUND_ONE_DOUBLE_DOWN_STAKE = 30;
  private static final int ROUND_ONE_FINAL_STAKE = 60;

  /** Current decision point in the Dice Event. */
  public enum Stage {
    ROUND_ONE_PREDICTION,
    ROUND_ONE_LUCKY_SEVEN,
    ROUND_ONE_DOUBLE_DOWN_PREDICTION,
    ROUND_ONE_FINAL_LUCKY_SEVEN,
    ROUND_ONE_FINAL_PREDICTION,
    STAKE_DECISION,
    ROUND_TWO_PREDICTION,
    ROUND_TWO_LUCKY_SEVEN,
    ROUND_TWO_DOUBLE_DOWN_PREDICTION,
    ROUND_TWO_FINAL_LUCKY_SEVEN,
    ROUND_TWO_FINAL_PREDICTION,
    RESOLVED
  }

  /** Current Dice Event round. */
  public enum Round {
    ONE,
    TWO,
    COMPLETE
  }

  private enum Prediction {
    LOW,
    HIGH
  }

  private final Random random;
  private final List<String> eligibleCardIds;

  private Stage stage = Stage.ROUND_ONE_PREDICTION;
  private int currentStake;
  private ChanceOutcome terminalOutcome;
  private String terminalChoiceId;
  private DiceRoll lastDiceRoll;
  private int rollSequence;

  /**
   * Creates Dice behaviour using the same eligible-card rule as the Wishing Fountain Event.
   *
   * <p>Eligible IDs come from all registered card definitions, with null definitions and blank IDs
   * ignored. IDs are deduplicated and sorted before deterministic random selection.
   *
   * @param random injected source used for dice and card selection
   * @param cardService authoritative source of registered card definitions
   * @throws IllegalArgumentException when the Card Service has no eligible card definitions
   */
  public DiceEncounterBehaviour(Random random, CardService cardService) {
    this.random = Objects.requireNonNull(random, "random cannot be null");
    Objects.requireNonNull(cardService, "cardService cannot be null");

    eligibleCardIds =
        cardService.getAllCards().stream()
            .filter(Objects::nonNull)
            .map(card -> card.id)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .sorted()
            .toList();
    if (eligibleCardIds.isEmpty()) {
      throw new IllegalArgumentException("Dice Event requires at least one registered card");
    }
  }

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    return switch (stage) {
      case ROUND_ONE_PREDICTION -> resolveRoundOnePrediction(choiceId);
      case ROUND_ONE_LUCKY_SEVEN -> resolveRoundOneLuckySeven(choiceId);
      case ROUND_ONE_DOUBLE_DOWN_PREDICTION -> resolveRoundOneDoubleDownPrediction(choiceId, false);
      case ROUND_ONE_FINAL_LUCKY_SEVEN -> resolveRoundOneFinalLuckySeven(choiceId);
      case ROUND_ONE_FINAL_PREDICTION -> resolveRoundOneDoubleDownPrediction(choiceId, true);
      case STAKE_DECISION -> resolveStakeDecision(choiceId);
      case ROUND_TWO_PREDICTION -> resolveRoundTwoPrediction(choiceId);
      case ROUND_TWO_LUCKY_SEVEN -> resolveRoundTwoLuckySeven(choiceId);
      case ROUND_TWO_DOUBLE_DOWN_PREDICTION -> resolveRoundTwoDoubleDownPrediction(choiceId, false);
      case ROUND_TWO_FINAL_LUCKY_SEVEN -> resolveRoundTwoFinalLuckySeven(choiceId);
      case ROUND_TWO_FINAL_PREDICTION -> resolveRoundTwoDoubleDownPrediction(choiceId, true);
      case RESOLVED ->
          Objects.equals(terminalChoiceId, choiceId)
              ? ChanceBehaviourResult.outcome(terminalOutcome)
              : ChanceBehaviourResult.invalidChoice();
    };
  }

  @Override
  public List<String> getAvailableChoiceIds(ChanceEncounter encounter) {
    return switch (stage) {
      case ROUND_ONE_PREDICTION,
              ROUND_ONE_DOUBLE_DOWN_PREDICTION,
              ROUND_ONE_FINAL_PREDICTION,
              ROUND_TWO_PREDICTION,
              ROUND_TWO_DOUBLE_DOWN_PREDICTION,
              ROUND_TWO_FINAL_PREDICTION ->
          List.of(LOW_CHOICE_ID, HIGH_CHOICE_ID);
      case ROUND_ONE_LUCKY_SEVEN,
              ROUND_ONE_FINAL_LUCKY_SEVEN,
              ROUND_TWO_LUCKY_SEVEN,
              ROUND_TWO_FINAL_LUCKY_SEVEN ->
          List.of(TAKE_CHOICE_ID, DOUBLE_DOWN_CHOICE_ID);
      case STAKE_DECISION -> List.of(CASH_OUT_CHOICE_ID, CONTINUE_CHOICE_ID);
      case RESOLVED -> List.of();
    };
  }

  @Override
  public String getChoicePrompt() {
    return switch (stage) {
      case ROUND_ONE_PREDICTION -> "ROUND 1 - PREDICT LOW OR HIGH";
      case ROUND_ONE_LUCKY_SEVEN, ROUND_ONE_FINAL_LUCKY_SEVEN ->
          "LUCKY SEVEN - TAKE THE OFFER OR DOUBLE DOWN";
      case ROUND_ONE_DOUBLE_DOWN_PREDICTION, ROUND_ONE_FINAL_PREDICTION ->
          "ROUND 1 DOUBLE DOWN - PREDICT AGAIN";
      case STAKE_DECISION -> "STAKE: " + currentStake + " GOLD - CASH OUT OR CONTINUE";
      case ROUND_TWO_PREDICTION -> "ROUND 2 - PREDICT LOW OR HIGH";
      case ROUND_TWO_LUCKY_SEVEN, ROUND_TWO_FINAL_LUCKY_SEVEN ->
          "ROUND 2 LUCKY SEVEN - TAKE OR DOUBLE DOWN";
      case ROUND_TWO_DOUBLE_DOWN_PREDICTION, ROUND_TWO_FINAL_PREDICTION ->
          "ROUND 2 DOUBLE DOWN - PREDICT AGAIN";
      case RESOLVED -> "WAGER COMPLETE";
    };
  }

  @Override
  public String getStageResultText() {
    return switch (stage) {
      case ROUND_ONE_LUCKY_SEVEN,
              ROUND_ONE_FINAL_LUCKY_SEVEN,
              ROUND_TWO_LUCKY_SEVEN,
              ROUND_TWO_FINAL_LUCKY_SEVEN ->
          "LUCKY SEVEN!\nThe dice total is 7. Take the offer or double down.";
      case STAKE_DECISION -> "Prediction won. Your current stake is " + currentStake + " gold.";
      case ROUND_TWO_PREDICTION ->
          "Round one is complete. Your " + currentStake + " gold stake is still at risk.";
      default -> ChanceEncounterBehaviour.super.getStageResultText();
    };
  }

  @Override
  public Optional<DiceRoll> getLastDiceRoll() {
    return Optional.ofNullable(lastDiceRoll);
  }

  /**
   * Returns the current decision point for UI integration and deterministic tests.
   *
   * @return current Dice stage
   */
  public Stage getStage() {
    return stage;
  }

  /**
   * Returns the Gold currently at risk. No Gold is applied until a terminal outcome is produced.
   *
   * @return current conceptual stake
   */
  public int getCurrentStake() {
    return currentStake;
  }

  /**
   * Returns the current round derived from the active decision point.
   *
   * @return active round, or {@link Round#COMPLETE} after terminal resolution
   */
  public Round getCurrentRound() {
    return switch (stage) {
      case ROUND_ONE_PREDICTION,
              ROUND_ONE_LUCKY_SEVEN,
              ROUND_ONE_DOUBLE_DOWN_PREDICTION,
              ROUND_ONE_FINAL_LUCKY_SEVEN,
              ROUND_ONE_FINAL_PREDICTION,
              STAKE_DECISION ->
          Round.ONE;
      case ROUND_TWO_PREDICTION,
              ROUND_TWO_LUCKY_SEVEN,
              ROUND_TWO_DOUBLE_DOWN_PREDICTION,
              ROUND_TWO_FINAL_LUCKY_SEVEN,
              ROUND_TWO_FINAL_PREDICTION ->
          Round.TWO;
      case RESOLVED -> Round.COMPLETE;
    };
  }

  private ChanceBehaviourResult resolveRoundOnePrediction(String choiceId) {
    Prediction prediction = predictionFor(choiceId);
    if (prediction == null) {
      return ChanceBehaviourResult.invalidChoice();
    }
    int total = rollDice();
    if (total == LUCKY_SEVEN) {
      stage = Stage.ROUND_ONE_LUCKY_SEVEN;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (!matches(prediction, total)) {
      return resolveLoss(choiceId);
    }

    currentStake = ROUND_ONE_NORMAL_STAKE;
    stage = Stage.STAKE_DECISION;
    return ChanceBehaviourResult.awaitingChoice();
  }

  private ChanceBehaviourResult resolveRoundOneDoubleDownPrediction(
      String choiceId, boolean finalTier) {
    Prediction prediction = predictionFor(choiceId);
    if (prediction == null) {
      return ChanceBehaviourResult.invalidChoice();
    }
    int total = rollDice();
    if (total == LUCKY_SEVEN) {
      if (!finalTier) {
        stage = Stage.ROUND_ONE_FINAL_LUCKY_SEVEN;
        return ChanceBehaviourResult.awaitingChoice();
      }
      currentStake = ROUND_ONE_FINAL_STAKE;
      stage = Stage.STAKE_DECISION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (!matches(prediction, total)) {
      return resolveLoss(choiceId);
    }

    currentStake = finalTier ? ROUND_ONE_FINAL_STAKE : ROUND_ONE_DOUBLE_DOWN_STAKE;
    stage = Stage.STAKE_DECISION;
    return ChanceBehaviourResult.awaitingChoice();
  }

  private ChanceBehaviourResult resolveRoundOneLuckySeven(String choiceId) {
    if (TAKE_CHOICE_ID.equals(choiceId)) {
      currentStake = ROUND_ONE_LUCKY_TAKE_STAKE;
      stage = Stage.STAKE_DECISION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (DOUBLE_DOWN_CHOICE_ID.equals(choiceId)) {
      stage = Stage.ROUND_ONE_DOUBLE_DOWN_PREDICTION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    return ChanceBehaviourResult.invalidChoice();
  }

  private ChanceBehaviourResult resolveRoundOneFinalLuckySeven(String choiceId) {
    if (TAKE_CHOICE_ID.equals(choiceId)) {
      currentStake = ROUND_ONE_DOUBLE_DOWN_STAKE;
      stage = Stage.STAKE_DECISION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (DOUBLE_DOWN_CHOICE_ID.equals(choiceId)) {
      stage = Stage.ROUND_ONE_FINAL_PREDICTION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    return ChanceBehaviourResult.invalidChoice();
  }

  private ChanceBehaviourResult resolveStakeDecision(String choiceId) {
    if (CASH_OUT_CHOICE_ID.equals(choiceId)) {
      return resolveTerminal(choiceId, new ChanceOutcome(0, currentStake));
    }
    if (CONTINUE_CHOICE_ID.equals(choiceId)) {
      stage = Stage.ROUND_TWO_PREDICTION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    return ChanceBehaviourResult.invalidChoice();
  }

  private ChanceBehaviourResult resolveRoundTwoPrediction(String choiceId) {
    Prediction prediction = predictionFor(choiceId);
    if (prediction == null) {
      return ChanceBehaviourResult.invalidChoice();
    }
    int total = rollDice();
    if (total == LUCKY_SEVEN) {
      stage = Stage.ROUND_TWO_LUCKY_SEVEN;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (!matches(prediction, total)) {
      return resolveLoss(choiceId);
    }

    return resolveTerminal(choiceId, rewardOutcome(1));
  }

  private ChanceBehaviourResult resolveRoundTwoDoubleDownPrediction(
      String choiceId, boolean finalTier) {
    Prediction prediction = predictionFor(choiceId);
    if (prediction == null) {
      return ChanceBehaviourResult.invalidChoice();
    }
    int total = rollDice();
    if (total == LUCKY_SEVEN && !finalTier) {
      stage = Stage.ROUND_TWO_FINAL_LUCKY_SEVEN;
      return ChanceBehaviourResult.awaitingChoice();
    }
    if (total != LUCKY_SEVEN && !matches(prediction, total)) {
      return resolveLoss(choiceId);
    }

    return resolveTerminal(choiceId, rewardOutcome(finalTier ? 3 : 2));
  }

  private ChanceBehaviourResult resolveRoundTwoLuckySeven(String choiceId) {
    if (TAKE_CHOICE_ID.equals(choiceId)) {
      return resolveTerminal(choiceId, rewardOutcome(1));
    }
    if (DOUBLE_DOWN_CHOICE_ID.equals(choiceId)) {
      stage = Stage.ROUND_TWO_DOUBLE_DOWN_PREDICTION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    return ChanceBehaviourResult.invalidChoice();
  }

  private ChanceBehaviourResult resolveRoundTwoFinalLuckySeven(String choiceId) {
    if (TAKE_CHOICE_ID.equals(choiceId)) {
      return resolveTerminal(choiceId, rewardOutcome(2));
    }
    if (DOUBLE_DOWN_CHOICE_ID.equals(choiceId)) {
      stage = Stage.ROUND_TWO_FINAL_PREDICTION;
      return ChanceBehaviourResult.awaitingChoice();
    }
    return ChanceBehaviourResult.invalidChoice();
  }

  private ChanceOutcome rewardOutcome(int cardCount) {
    List<String> cards =
        java.util.stream.IntStream.range(0, cardCount)
            .mapToObj(ignored -> eligibleCardIds.get(random.nextInt(eligibleCardIds.size())))
            .toList();
    return ChanceOutcome.withCardRewards(0, currentStake, cards);
  }

  private ChanceBehaviourResult resolveTerminal(String choiceId, ChanceOutcome outcome) {
    stage = Stage.RESOLVED;
    terminalChoiceId = choiceId;
    terminalOutcome = outcome;
    return ChanceBehaviourResult.outcome(outcome);
  }

  private ChanceBehaviourResult resolveLoss(String choiceId) {
    currentStake = 0;
    return resolveTerminal(choiceId, new ChanceOutcome(0, 0));
  }

  private Prediction predictionFor(String choiceId) {
    if (LOW_CHOICE_ID.equals(choiceId)) {
      return Prediction.LOW;
    }
    if (HIGH_CHOICE_ID.equals(choiceId)) {
      return Prediction.HIGH;
    }
    return null;
  }

  private boolean matches(Prediction prediction, int total) {
    return prediction == Prediction.LOW ? total <= 6 : total >= 8;
  }

  private int rollDice() {
    int firstDie = random.nextInt(DIE_SIDES) + 1;
    int secondDie = random.nextInt(DIE_SIDES) + 1;
    lastDiceRoll = new DiceRoll(firstDie, secondDie, ++rollSequence);
    return lastDiceRoll.total();
  }
}
