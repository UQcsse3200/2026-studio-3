package com.csse3200.game.chance;

import java.util.Objects;

/** Player-independent result produced by Chance Encounter behaviour. */
public final class ChanceBehaviourResult {
  /** Kind of result produced while resolving an encounter choice. */
  public enum Type {
    /** A normal outcome is ready to be applied to the player. */
    OUTCOME,
    /** The selected choice does not exist for this behaviour. */
    INVALID_CHOICE,
    /** The choice advanced a staged encounter and another player choice is required. */
    AWAITING_CHOICE,
    /** Another flow has accepted the choice and will finish the encounter later. */
    DELEGATED
  }

  private static final ChanceBehaviourResult INVALID_CHOICE_RESULT =
      new ChanceBehaviourResult(Type.INVALID_CHOICE, null);
  private static final ChanceBehaviourResult AWAITING_CHOICE_RESULT =
      new ChanceBehaviourResult(Type.AWAITING_CHOICE, null);
  private static final ChanceBehaviourResult DELEGATED_RESULT =
      new ChanceBehaviourResult(Type.DELEGATED, null);

  private final Type type;
  private final ChanceOutcome outcome;

  private ChanceBehaviourResult(Type type, ChanceOutcome outcome) {
    this.type = type;
    this.outcome = outcome;
  }

  /**
   * Creates a normal result whose outcome should be applied through the existing outcome applier.
   *
   * @param outcome resolved encounter outcome
   * @return outcome result
   */
  public static ChanceBehaviourResult outcome(ChanceOutcome outcome) {
    return new ChanceBehaviourResult(
        Type.OUTCOME, Objects.requireNonNull(outcome, "outcome cannot be null"));
  }

  /**
   * Creates a result for an unknown or unavailable choice.
   *
   * @return shared invalid-choice result
   */
  public static ChanceBehaviourResult invalidChoice() {
    return INVALID_CHOICE_RESULT;
  }

  /**
   * Creates a result indicating that a staged encounter accepted the choice and needs another one.
   *
   * @return shared awaiting-choice result
   */
  public static ChanceBehaviourResult awaitingChoice() {
    return AWAITING_CHOICE_RESULT;
  }

  /**
   * Creates a result indicating that another flow will finish this encounter later.
   *
   * <p>This result does not define or implement the delegated flow.
   *
   * @return shared delegated result
   */
  public static ChanceBehaviourResult delegated() {
    return DELEGATED_RESULT;
  }

  /**
   * Returns the kind of behaviour result.
   *
   * @return result type
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the resolved normal outcome.
   *
   * @return outcome for {@link Type#OUTCOME}, otherwise null
   */
  public ChanceOutcome getOutcome() {
    return outcome;
  }
}
