package com.csse3200.game.encounters.integration;

import java.util.Objects;

/**
 * Coordinates the Player, Card Library, and Deck systems for a transaction-safe Shop purchase.
 *
 * <p>The deck is updated before currency is committed. If the currency update fails, the added card
 * is removed and the original currency is restored.
 */
public final class IntegratedShopTransactionGateway implements ShopTransactionGateway {
  private final PlayerStateGateway player;
  private final CardCatalogGateway cardCatalog;
  private final DeckGateway deck;

  /**
   * Creates the transaction coordinator for one Player, Card Library, and Deck combination.
   *
   * @param player player state used for the price check and currency update
   * @param cardCatalog Team 6 card lookup boundary
   * @param deck Team 5 persistent deck boundary
   */
  public IntegratedShopTransactionGateway(
      PlayerStateGateway player, CardCatalogGateway cardCatalog, DeckGateway deck) {
    this.player = Objects.requireNonNull(player, "player cannot be null");
    this.cardCatalog = Objects.requireNonNull(cardCatalog, "cardCatalog cannot be null");
    this.deck = Objects.requireNonNull(deck, "deck cannot be null");
  }

  @Override
  public int getCurrency() {
    return player.getCurrency();
  }

  @Override
  public ShopTransactionStatus validatePurchase(String cardId, int price) {
    if (cardId == null || cardId.isBlank()) {
      return ShopTransactionStatus.INVALID_CARD;
    }
    if (price < 0) {
      return ShopTransactionStatus.INVALID_PRICE;
    }

    boolean cardExists;
    try {
      cardExists = cardCatalog.containsCard(cardId);
    } catch (RuntimeException exception) {
      return ShopTransactionStatus.CARD_NOT_FOUND;
    }
    if (!cardExists) {
      return ShopTransactionStatus.CARD_NOT_FOUND;
    }
    if (player.getCurrency() < price) {
      return ShopTransactionStatus.INSUFFICIENT_CURRENCY;
    }
    return ShopTransactionStatus.READY;
  }

  @Override
  public ShopTransactionStatus purchaseCard(String cardId, int price) {
    ShopTransactionStatus validation = validatePurchase(cardId, price);
    if (validation != ShopTransactionStatus.READY) {
      return validation;
    }

    int currencyBefore = player.getCurrency();
    boolean cardAdded;
    try {
      cardAdded = deck.addCard(cardId);
    } catch (RuntimeException exception) {
      return ShopTransactionStatus.CARD_ADD_FAILED;
    }
    if (!cardAdded) {
      return ShopTransactionStatus.CARD_ADD_FAILED;
    }

    try {
      player.setCurrency(currencyBefore - price);
      if (player.getCurrency() != currencyBefore - price) {
        throw new IllegalStateException("Player currency update was not accepted");
      }
    } catch (RuntimeException exception) {
      return rollback(cardId, currencyBefore);
    }

    return ShopTransactionStatus.SUCCESS;
  }

  private ShopTransactionStatus rollback(String cardId, int currencyBefore) {
    boolean cardRemoved;
    try {
      cardRemoved = deck.removeCard(cardId);
    } catch (RuntimeException exception) {
      cardRemoved = false;
    }

    boolean currencyRestored;
    try {
      player.setCurrency(currencyBefore);
      currencyRestored = player.getCurrency() == currencyBefore;
    } catch (RuntimeException exception) {
      currencyRestored = false;
    }

    return cardRemoved && currencyRestored
        ? ShopTransactionStatus.CURRENCY_UPDATE_FAILED
        : ShopTransactionStatus.ROLLBACK_FAILED;
  }
}
