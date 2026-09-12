package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceOutcome;

/** Immutable result of selecting and applying a Chance Encounter choice. */
public final class ChanceResolution {
  /** Status of a Chance Encounter resolution attempt. */
  public enum Status {
    /** The outcome was committed to the player. */
    APPLIED,
    /** No choice with the supplied identifier exists. */
    INVALID_CHOICE,
    /** This session already applied a successful choice. */
    ALREADY_RESOLVED,
    /** The session has already completed or been cancelled. */
    ENCOUNTER_CLOSED,
    /** The selected choice did not contain an outcome. */
    INVALID_OUTCOME,
    /** Applying the outcome would make currency negative. */
    INSUFFICIENT_CURRENCY,
    /** Applying the outcome would overflow an integer player statistic. */
    ARITHMETIC_OVERFLOW,
    /** A Player update failed and all original values were restored. */
    PLAYER_UPDATE_FAILED,
    /** A Player update failed and at least one original value could not be restored. */
    ROLLBACK_FAILED
  }

  private final Status status;
  private final ChanceOutcome outcome;
  private final int healthBefore;
  private final int healthAfter;
  private final int currencyBefore;
  private final int currencyAfter;
  private final String message;

  private ChanceResolution(
      Status status,
      ChanceOutcome outcome,
      int healthBefore,
      int healthAfter,
      int currencyBefore,
      int currencyAfter,
      String message) {
    this.status = status;
    this.outcome = outcome;
    this.healthBefore = healthBefore;
    this.healthAfter = healthAfter;
    this.currencyBefore = currencyBefore;
    this.currencyAfter = currencyAfter;
    this.message = message;
  }

  static ChanceResolution applied(
      ChanceOutcome outcome,
      int healthBefore,
      int healthAfter,
      int currencyBefore,
      int currencyAfter) {
    return new ChanceResolution(
        Status.APPLIED,
        outcome,
        healthBefore,
        healthAfter,
        currencyBefore,
        currencyAfter,
        "Chance outcome applied.");
  }

  static ChanceResolution failure(
      Status status, ChanceOutcome outcome, int health, int currency, String message) {
    return new ChanceResolution(
        status, outcome, health, health, currency, currency, message == null ? "" : message);
  }

  /**
   * Reports whether this resolution committed its outcome.
   *
   * @return true when the outcome was applied to the player
   */
  public boolean isSuccess() {
    return status == Status.APPLIED;
  }

  /**
   * Returns the resolution status.
   *
   * @return resolution status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Returns the selected outcome.
   *
   * @return selected outcome, or null when no valid choice was resolved
   */
  public ChanceOutcome getOutcome() {
    return outcome;
  }

  /**
   * Returns the health snapshot taken before resolution.
   *
   * @return health before the resolution attempt
   */
  public int getHealthBefore() {
    return healthBefore;
  }

  /**
   * Returns the health snapshot taken after resolution.
   *
   * @return health after the resolution attempt
   */
  public int getHealthAfter() {
    return healthAfter;
  }

  /**
   * Returns the currency snapshot taken before resolution.
   *
   * @return currency before the resolution attempt
   */
  public int getCurrencyBefore() {
    return currencyBefore;
  }

  /**
   * Returns the currency snapshot taken after resolution.
   *
   * @return currency after the resolution attempt
   */
  public int getCurrencyAfter() {
    return currencyAfter;
  }

  /**
   * Returns a displayable explanation of the resolution status.
   *
   * @return player-facing or diagnostic resolution message
   */
  public String getMessage() {
    return message;
  }
}
