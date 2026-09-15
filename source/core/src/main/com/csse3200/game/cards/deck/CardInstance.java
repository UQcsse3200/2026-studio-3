package com.csse3200.game.cards.deck;

import java.util.UUID;

/**
 * One physical copy of a card in a {@link PlayerDeck}.
 *
 * <p>Two cards can share the same {@code cardId} (e.g. two "strike" cards), but each copy gets its
 * own {@code instanceId} the moment it is added to a deck, so callers that need to tell duplicate
 * copies apart (cooldown tracking, deck rearranging) have something to key off other than the
 * shared card ID.
 */
public record CardInstance(String cardId, UUID instanceId) {

  public CardInstance {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("cardId must not be null or blank");
    }
    if (instanceId == null) {
      throw new IllegalArgumentException("instanceId must not be null");
    }
  }

  /** Creates a new instance of the given card with a freshly generated unique instance ID. */
  public static CardInstance of(String cardId) {
    return new CardInstance(cardId, UUID.randomUUID());
  }
}
