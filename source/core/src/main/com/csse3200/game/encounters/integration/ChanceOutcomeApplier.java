package com.csse3200.game.encounters.integration;

import com.csse3200.game.chance.ChanceOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies Chance Encounter outcomes to a player without coupling Chance logic to Player classes.
 */
public final class ChanceOutcomeApplier {
  private final PlayerStateGateway player;
  private final CardCatalogGateway cardCatalog;
  private final DeckGateway deck;

  /**
   * Creates an outcome applier for one player boundary.
   *
   * @param player player-state boundary receiving encounter changes
   */
  public ChanceOutcomeApplier(PlayerStateGateway player) {
    this.player = Objects.requireNonNull(player, "player cannot be null");
    this.cardCatalog = null;
    this.deck = null;
  }

  /**
   * Creates an outcome applier with production card-catalog and persistent-deck boundaries.
   *
   * @param player player-state boundary receiving encounter changes
   * @param cardCatalog authoritative card lookup boundary
   * @param deck persistent player deck receiving card rewards
   */
  public ChanceOutcomeApplier(
      PlayerStateGateway player, CardCatalogGateway cardCatalog, DeckGateway deck) {
    this.player = Objects.requireNonNull(player, "player cannot be null");
    this.cardCatalog = Objects.requireNonNull(cardCatalog, "cardCatalog cannot be null");
    this.deck = Objects.requireNonNull(deck, "deck cannot be null");
  }

  /**
   * Applies card, currency, and health changes as one logical operation.
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
    List<String> cardRewardIds = outcome.getCardRewardIds();

    ChanceResolution cardValidation = validateCardRewards(outcome, cardRewardIds);
    if (cardValidation != null) {
      return cardValidation;
    }

    List<String> addedCardIds = new ArrayList<>();
    for (String cardRewardId : cardRewardIds) {
      boolean cardAdded;
      try {
        cardAdded = deck.addCard(cardRewardId);
      } catch (RuntimeException exception) {
        cardAdded = false;
      }
      if (!cardAdded) {
        boolean rollbackSucceeded = rollbackCardAdditions(addedCardIds);
        return failure(
            rollbackSucceeded
                ? ChanceResolution.Status.CARD_ADD_FAILED
                : ChanceResolution.Status.ROLLBACK_FAILED,
            outcome,
            rollbackSucceeded
                ? "A reward card could not be added to the player's deck; no cards were kept."
                : "A reward card could not be added and earlier card additions could not be fully"
                    + " rolled back; manual recovery is required.");
      }
      addedCardIds.add(cardRewardId);
    }

    try {
      if (outcome.getCurrencyDelta() != 0) {
        player.setCurrency(currencyTarget);
      }
      if (player.getCurrency() != currencyTarget) {
        throw new IllegalStateException("Player currency update was not accepted");
      }
    } catch (RuntimeException exception) {
      boolean rollbackSucceeded = rollbackBeforeHealth(addedCardIds, currencyBefore);
      return failure(
          rollbackSucceeded
              ? ChanceResolution.Status.PLAYER_UPDATE_FAILED
              : ChanceResolution.Status.ROLLBACK_FAILED,
          outcome,
          rollbackSucceeded
              ? "The player currency could not be updated; no encounter changes were kept."
              : "The outcome could not update currency or fully roll back; manual recovery is"
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
      if (healthAfter == healthBefore) {
        boolean rollbackSucceeded = rollbackBeforeHealth(addedCardIds, currencyBefore);
        return failure(
            rollbackSucceeded
                ? ChanceResolution.Status.PLAYER_UPDATE_FAILED
                : ChanceResolution.Status.ROLLBACK_FAILED,
            outcome,
            rollbackSucceeded
                ? "The direct health update was not accepted; earlier outcome changes were rolled "
                    + "back."
                : "The direct health update was not accepted and earlier changes could not be "
                    + "fully rolled back; manual recovery is required.");
      }

      commitCardsAfterIrreversibleHealthFailure(addedCardIds);
      return ChanceResolution.failure(
          ChanceResolution.Status.ROLLBACK_FAILED,
          outcome,
          healthBefore,
          healthAfter,
          currencyBefore,
          player.getCurrency(),
          "The direct health update failed after health changed; earlier outcome changes were "
              + "kept because rollback may be unsafe after health events began.");
    }

    if (!addedCardIds.isEmpty()) {
      try {
        commitCardAdditions(addedCardIds);
      } catch (RuntimeException exception) {
        if (player.getHealth() == healthBefore) {
          boolean rollbackSucceeded = rollbackBeforeHealth(addedCardIds, currencyBefore);
          return failure(
              rollbackSucceeded
                  ? ChanceResolution.Status.CARD_ADD_FAILED
                  : ChanceResolution.Status.ROLLBACK_FAILED,
              outcome,
              rollbackSucceeded
                  ? "The reward card could not be committed; no encounter changes were kept."
                  : "The reward card commit and rollback both failed; manual recovery is"
                      + " required.");
        }
        return ChanceResolution.failure(
            ChanceResolution.Status.ROLLBACK_FAILED,
            outcome,
            healthBefore,
            player.getHealth(),
            currencyBefore,
            player.getCurrency(),
            "The reward card could not be committed after health changed; automatic rollback was "
                + "skipped because health events may be irreversible.");
      }
    }

    return ChanceResolution.applied(
        outcome, healthBefore, player.getHealth(), currencyBefore, player.getCurrency());
  }

  ChanceResolution failure(ChanceResolution.Status status, ChanceOutcome outcome, String message) {
    return ChanceResolution.failure(
        status, outcome, player.getHealth(), player.getCurrency(), message);
  }

  private ChanceResolution validateCardRewards(ChanceOutcome outcome, List<String> cardRewardIds) {
    if (cardRewardIds.isEmpty()) {
      return null;
    }
    if (cardCatalog == null || deck == null) {
      return failure(
          ChanceResolution.Status.CARD_NOT_FOUND,
          outcome,
          "Card reward services are not available for this encounter.");
    }

    for (String cardRewardId : cardRewardIds) {
      if (cardRewardId == null || cardRewardId.isBlank()) {
        return failure(
            ChanceResolution.Status.INVALID_CARD_REWARD,
            outcome,
            "The reward card identifier must not be blank.");
      }

      boolean cardExists;
      try {
        cardExists = cardCatalog.containsCard(cardRewardId);
      } catch (RuntimeException exception) {
        cardExists = false;
      }
      if (!cardExists) {
        return failure(
            ChanceResolution.Status.CARD_NOT_FOUND,
            outcome,
            "The reward card is not registered: " + cardRewardId);
      }

      try {
        if (!deck.canAddCard(cardRewardId)) {
          return failure(
              ChanceResolution.Status.CARD_ADD_FAILED,
              outcome,
              "The player's deck cannot accept the reward card.");
        }
      } catch (RuntimeException exception) {
        return failure(
            ChanceResolution.Status.CARD_ADD_FAILED,
            outcome,
            "The player's deck could not validate the reward card.");
      }
    }
    return null;
  }

  private boolean rollbackBeforeHealth(List<String> cardRewardIds, int currency) {
    boolean cardRestored = rollbackCardAdditions(cardRewardIds);
    boolean currencyRestored = rollbackCurrency(currency);
    return cardRestored && currencyRestored;
  }

  private boolean rollbackCardAdditions(List<String> cardRewardIds) {
    boolean cardRestored = true;
    for (int index = cardRewardIds.size() - 1; index >= 0; index--) {
      try {
        cardRestored = deck.rollbackCardAddition(cardRewardIds.get(index)) && cardRestored;
      } catch (RuntimeException exception) {
        cardRestored = false;
      }
    }
    return cardRestored;
  }

  private void commitCardsAfterIrreversibleHealthFailure(List<String> cardRewardIds) {
    try {
      commitCardAdditions(cardRewardIds);
    } catch (RuntimeException ignored) {
      // The outcome is already a partial failure and health-event rollback is unsafe.
    }
  }

  private void commitCardAdditions(List<String> cardRewardIds) {
    for (int index = cardRewardIds.size() - 1; index >= 0; index--) {
      deck.commitCardAddition(cardRewardIds.get(index));
    }
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
