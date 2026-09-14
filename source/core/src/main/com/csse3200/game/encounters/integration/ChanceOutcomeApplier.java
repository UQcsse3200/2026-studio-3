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
   * <p>Currency is never allowed to become negative. Currency is committed and verified before the
   * direct health delta is applied. Health is the final mutation because lethal direct health loss
   * can emit an irreversible death event.
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

    long requestedCurrency = (long) currencyBefore + outcome.getCurrencyDelta();
    if (requestedCurrency > Integer.MAX_VALUE) {
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

    long requestedHealth = (long) healthBefore + outcome.getHealthDelta();
    int healthTarget = (int) Math.max(0L, Math.min((long) player.getMaxHealth(), requestedHealth));
    int appliedHealthDelta = healthTarget - healthBefore;
    int currencyTarget = (int) requestedCurrency;

    try {
      if (outcome.getCurrencyDelta() != 0) {
        player.setCurrency(currencyTarget);
      }
      if (player.getCurrency() != currencyTarget) {
        throw new IllegalStateException("Player currency update was not accepted");
      }
    } catch (RuntimeException exception) {
      boolean rollbackSucceeded = rollbackCurrency(currencyBefore);
      return failure(
          rollbackSucceeded
              ? ChanceResolution.Status.PLAYER_UPDATE_FAILED
              : ChanceResolution.Status.ROLLBACK_FAILED,
          outcome,
          rollbackSucceeded
              ? "The player currency could not be updated; no encounter changes were kept."
              : "The player currency update and rollback both failed; manual recovery is"
                  + " required.");
    }

    try {
      if (appliedHealthDelta != 0) {
        player.applyDirectHealthChange(appliedHealthDelta);
      }
      if (player.getHealth() != healthTarget) {
        throw new IllegalStateException("Player health update was not accepted");
      }
    } catch (RuntimeException exception) {
      int healthAfter = player.getHealth();
      int currencyAfter = player.getCurrency();
      boolean stateUnchanged = healthAfter == healthBefore && currencyAfter == currencyBefore;
      return ChanceResolution.failure(
          stateUnchanged
              ? ChanceResolution.Status.PLAYER_UPDATE_FAILED
              : ChanceResolution.Status.ROLLBACK_FAILED,
          outcome,
          healthBefore,
          healthAfter,
          currencyBefore,
          currencyAfter,
          stateUnchanged
              ? "The direct health update was not accepted; player state was unchanged."
              : "The direct health update failed after mutation began; automatic rollback was "
                  + "skipped because the health change may have emitted an irreversible death "
                  + "event.");
    }

    return ChanceResolution.applied(
        outcome, healthBefore, player.getHealth(), currencyBefore, player.getCurrency());
  }

  ChanceResolution failure(ChanceResolution.Status status, ChanceOutcome outcome, String message) {
    return ChanceResolution.failure(
        status, outcome, player.getHealth(), player.getCurrency(), message);
  }

  private boolean rollbackCurrency(int currency) {
    try {
      player.setCurrency(currency);
      return player.getCurrency() == currency;
    } catch (RuntimeException ignored) {
      return false;
    }
  }
}
