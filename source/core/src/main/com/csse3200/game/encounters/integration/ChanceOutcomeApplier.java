package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceOutcome;
import java.util.Objects;

/**
 * Applies Chance Encounter outcomes to a player without coupling Chance logic to Player classes.
 */
public final class ChanceOutcomeApplier {
  private final PlayerStateGateway player;

  /**
   * Creates an outcome applier for one player boundary.
   *
   * @param player player-state boundary receiving encounter changes
   */
  public ChanceOutcomeApplier(PlayerStateGateway player) {
    this.player = Objects.requireNonNull(player, "player cannot be null");
  }

  /**
   * Applies both health and currency changes as one logical operation.
   *
   * <p>Currency is never allowed to become negative. If validation or an update fails, neither part
   * of the outcome is retained.
   *
   * @param outcome Player-independent result returned by Chance logic
   * @return detailed resolution status and before/after values
   */
  public ChanceResolution apply(ChanceOutcome outcome) {
    int healthBefore = player.getHealth();
    int currencyBefore = player.getCurrency();

    if (outcome == null) {
      return failure(
          ChanceResolution.Status.INVALID_OUTCOME,
          null,
          "The selected choice did not provide an outcome.");
    }

    long requestedHealth = (long) healthBefore + outcome.getHealthDelta();
    long requestedCurrency = (long) currencyBefore + outcome.getCurrencyDelta();
    if (requestedHealth > Integer.MAX_VALUE || requestedCurrency > Integer.MAX_VALUE) {
      return failure(
          ChanceResolution.Status.ARITHMETIC_OVERFLOW,
          outcome,
          "The outcome exceeds the supported player-stat range.");
    }
    if (requestedCurrency < 0) {
      return failure(
          ChanceResolution.Status.INSUFFICIENT_CURRENCY,
          outcome,
          "The player cannot afford this choice.");
    }

    int healthTarget = (int) Math.max(0L, requestedHealth);
    int currencyTarget = (int) requestedCurrency;
    try {
      player.setHealth(healthTarget);
      player.setCurrency(currencyTarget);
      if (player.getHealth() != healthTarget || player.getCurrency() != currencyTarget) {
        throw new IllegalStateException("Player state update was not accepted");
      }
    } catch (RuntimeException exception) {
      boolean rollbackSucceeded = rollback(healthBefore, currencyBefore);
      return failure(
          rollbackSucceeded
              ? ChanceResolution.Status.PLAYER_UPDATE_FAILED
              : ChanceResolution.Status.ROLLBACK_FAILED,
          outcome,
          rollbackSucceeded
              ? "The player state could not be updated; no encounter changes were kept."
              : "The player state update and rollback both failed; manual recovery is required.");
    }

    return ChanceResolution.applied(
        outcome, healthBefore, player.getHealth(), currencyBefore, player.getCurrency());
  }

  ChanceResolution failure(ChanceResolution.Status status, ChanceOutcome outcome, String message) {
    return ChanceResolution.failure(
        status, outcome, player.getHealth(), player.getCurrency(), message);
  }

  private boolean rollback(int health, int currency) {
    boolean healthRestored;
    try {
      player.setHealth(health);
      healthRestored = player.getHealth() == health;
    } catch (RuntimeException ignored) {
      healthRestored = false;
    }

    boolean currencyRestored;
    try {
      player.setCurrency(currency);
      currencyRestored = player.getCurrency() == currency;
    } catch (RuntimeException ignored) {
      currencyRestored = false;
    }
    return healthRestored && currencyRestored;
  }
}
