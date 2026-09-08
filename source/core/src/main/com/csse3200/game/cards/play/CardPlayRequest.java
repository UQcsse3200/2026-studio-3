package com.csse3200.game.cards.play;

/** Immutable request sent by Team 3/battle flow when a player attempts to play one card. */
public record CardPlayRequest(String cardId, CardPlayTarget target) {
  public CardPlayRequest {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (!cardId.equals(cardId.trim())) {
      throw new IllegalArgumentException("Card ID cannot have leading or trailing whitespace");
    }
    if (target == null) {
      throw new IllegalArgumentException("Card play target cannot be null");
    }
  }

  /** Creates a request for a self-targeting card. */
  public static CardPlayRequest self(String cardId) {
    return new CardPlayRequest(cardId, CardPlayTarget.self());
  }

  /** Creates a request for a card targeting one selected enemy. */
  public static CardPlayRequest singleEnemy(String cardId, String targetId) {
    return new CardPlayRequest(cardId, CardPlayTarget.singleEnemy(targetId));
  }

  /** Creates a request for a card targeting every enemy. */
  public static CardPlayRequest allEnemies(String cardId) {
    return new CardPlayRequest(cardId, CardPlayTarget.allEnemies());
  }
}
