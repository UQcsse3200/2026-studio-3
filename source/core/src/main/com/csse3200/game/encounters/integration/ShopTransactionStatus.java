package com.csse3200.game.encounters.integration;

/** Result of validating or executing a cross-system shop transaction. */
public enum ShopTransactionStatus {
  /** Validation succeeded and the transaction may be attempted. */
  READY,
  /** Card and currency changes were committed. */
  SUCCESS,
  /** Card identifier was null or blank. */
  INVALID_CARD,
  /** Price was negative. */
  INVALID_PRICE,
  /** Player cannot afford the price. */
  INSUFFICIENT_CURRENCY,
  /** Card Library has no matching card definition. */
  CARD_NOT_FOUND,
  /** Deck rejected the new card. */
  CARD_ADD_FAILED,
  /** Currency update failed and the deck change was rolled back. */
  CURRENCY_UPDATE_FAILED,
  /** At least one rollback operation failed. */
  ROLLBACK_FAILED,
  /** Legacy transaction failed without a more specific reason. */
  TRANSACTION_FAILED
}
