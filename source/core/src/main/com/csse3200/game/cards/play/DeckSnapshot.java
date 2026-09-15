package com.csse3200.game.cards.play;

import com.csse3200.game.cards.deck.BattleDeck;
import java.util.List;

/** Immutable battle-deck state returned after a card play attempt. */
public record DeckSnapshot(
    List<String> updatedHand, List<String> updatedDrawPile, List<String> updatedDiscardPile) {
  public DeckSnapshot {
    if (updatedHand == null || updatedDrawPile == null || updatedDiscardPile == null) {
      throw new IllegalArgumentException("Deck snapshot lists cannot be null");
    }
    updatedHand = List.copyOf(updatedHand);
    updatedDrawPile = List.copyOf(updatedDrawPile);
    updatedDiscardPile = List.copyOf(updatedDiscardPile);
  }

  /** Captures the current state of a battle deck. */
  public static DeckSnapshot from(BattleDeck battleDeck) {
    if (battleDeck == null) {
      throw new IllegalArgumentException("Battle deck cannot be null");
    }
    return new DeckSnapshot(
        battleDeck.getHand(), battleDeck.getDrawPile(), battleDeck.getDiscardPile());
  }

  /**
   * @return an empty snapshot for backwards-compatible result construction
   */
  public static DeckSnapshot empty() {
    return new DeckSnapshot(List.of(), List.of(), List.of());
  }
}
