package com.csse3200.game.encounters.integration;

/** Boundary used to add purchased or rewarded cards to the player's persistent deck. */
public interface DeckGateway {
  /**
   * Checks whether the persistent deck currently accepts a card.
   *
   * <p>The default keeps existing adapters compatible. Production deck adapters should override
   * this method when they enforce registered-card, duplicate, or size rules.
   *
   * @param cardId stable card identifier
   * @return true when an add may be attempted
   */
  default boolean canAddCard(String cardId) {
    return cardId != null && !cardId.isBlank();
  }

  /**
   * Adds one card to the player's deck.
   *
   * @param cardId stable card identifier
   * @return true when the card was added
   */
  boolean addCard(String cardId);

  /**
   * Removes one copy of a card. This operation is used to roll back a failed shop transaction.
   *
   * @param cardId stable card identifier
   * @return true when one copy was removed
   */
  boolean removeCard(String cardId);

  /**
   * Marks the most recent matching addition as committed.
   *
   * <p>Stateful adapters may use this hook to discard rollback metadata.
   *
   * @param cardId committed card identifier
   */
  default void commitCardAddition(String cardId) {}

  /**
   * Reverts the most recent matching addition after another transaction step fails.
   *
   * @param cardId card identifier added by the current transaction
   * @return true when the addition was reverted
   */
  default boolean rollbackCardAddition(String cardId) {
    return removeCard(cardId);
  }
}
