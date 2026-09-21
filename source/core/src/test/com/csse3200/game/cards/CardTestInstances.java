package com.csse3200.game.cards;

import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.stream.Stream;

/**
 * Selects a fixture copy for older single-copy regression tests. Duplicate-copy tests use explicit
 * IDs.
 */
public final class CardTestInstances {
  private CardTestInstances() {}

  public static String id(BattleDeck deck, String cardId) {
    return Stream.of(deck.getHand(), deck.getDrawPile(), deck.getDiscardPile())
        .flatMap(java.util.List::stream)
        .filter(card -> card.cardId().equals(cardId))
        .map(CardInstance::instanceId)
        .findFirst()
        .orElseThrow();
  }
}
