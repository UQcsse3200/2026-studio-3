package com.csse3200.game.cards.play;

import com.csse3200.game.cards.deck.BattleDeck;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks the player-round cooldown for cards sitting in the discard pile, automatically retrieving
 * a card straight back into the hand once its cooldown elapses. Owned by {@link CardPlayService}.
 */
final class CardCooldownTracker {
  private final BattleDeck battleDeck;
  private final List<Entry> entries = new ArrayList<>();

  CardCooldownTracker(BattleDeck battleDeck) {
    if (battleDeck == null) {
      throw new IllegalArgumentException("Battle deck cannot be null");
    }
    this.battleDeck = battleDeck;
  }

  /**
   * Starts tracking a just-discarded card's cooldown.
   *
   * @param cardId ID of the card that was just moved to the discard pile
   * @param rounds player rounds until it is retrieved; entries at 0 or below are never tracked
   */
  void trackDiscard(String cardId, int rounds) {
    if (rounds > 0) {
      entries.add(new Entry(cardId, rounds));
    }
  }

  /**
   * Ticks every tracked cooldown down by one player round, retrieving any card that reaches zero
   * straight back into the hand.
   *
   * @return IDs of cards retrieved this tick, in the order they were retrieved (empty if none)
   */
  List<String> tickRoundAndRetrieve() {
    List<String> retrieved = new ArrayList<>();
    Iterator<Entry> iterator = entries.iterator();
    while (iterator.hasNext()) {
      Entry entry = iterator.next();
      entry.roundsRemaining--;
      if (entry.roundsRemaining <= 0) {
        if (battleDeck.retrieveFromDiscard(entry.cardId)) {
          retrieved.add(entry.cardId);
        }
        iterator.remove();
      }
    }
    return retrieved;
  }

  private static final class Entry {
    private final String cardId;
    private int roundsRemaining;

    private Entry(String cardId, int roundsRemaining) {
      this.cardId = cardId;
      this.roundsRemaining = roundsRemaining;
    }
  }
}
