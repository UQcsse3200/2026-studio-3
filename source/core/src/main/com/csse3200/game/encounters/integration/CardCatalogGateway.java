package com.csse3200.game.encounters.integration;

/** Read-only boundary to the Cards / Library system owned by Team 6. */
@FunctionalInterface
public interface CardCatalogGateway {
  /**
   * Checks whether a card definition exists.
   *
   * @param cardId stable card identifier
   * @return true when the card can be resolved by the card library
   */
  boolean containsCard(String cardId);
}
