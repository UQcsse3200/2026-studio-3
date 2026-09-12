package com.csse3200.game.encounters.integration;

/** Cross-team boundary used by the Shop system to validate and complete card purchases. */
public interface ShopTransactionGateway {
  /**
   * Returns the current spendable Player currency.
   *
   * @return current player currency
   */
  int getCurrency();

  /**
   * Validates a purchase without changing player or deck state.
   *
   * @param cardId stable card identifier
   * @param price non-negative purchase price
   * @return {@link ShopTransactionStatus#READY} when the purchase can be attempted
   */
  ShopTransactionStatus validatePurchase(String cardId, int price);

  /**
   * Attempts the player, card-library, and deck parts of a purchase as one logical transaction.
   *
   * @param cardId stable card identifier
   * @param price non-negative purchase price
   * @return detailed transaction status
   */
  ShopTransactionStatus purchaseCard(String cardId, int price);

  /**
   * Returns the player's current permanent shop discount, as a fraction (e.g. 0.1f = 10% off).
   *
   * @return the current shop discount rate
   */
  float getShopDiscount();
}
