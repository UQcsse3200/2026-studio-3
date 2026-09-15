package com.csse3200.game.encounters.integration;

/** Boundary used to add purchased or rewarded cards to the player's persistent deck. */
public interface DeckGateway {
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
}
