package com.csse3200.game.cards.play;

import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.CardInstance;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks the player-round cooldown for cards sitting in the discard pile, automatically retrieving
 * a card straight back into the hand once its cooldown elapses. Owned by {@link CardPlayService}.
 *
 * <p>Cooldowns are keyed by {@link CardInstance} (card ID plus a unique instance ID), not by bare
 * card ID. Two copies of the same card can be discarded independently — e.g. one on cooldown, one
 * still playable — and only the exact instance that was actually discarded comes off cooldown and
 * is retrieved, regardless of how many other copies of the same card are also in the discard pile.
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
   * @param instance the exact card instance that was just moved to the discard pile
   * @param rounds player rounds until it is retrieved; entries at 0 or below are never tracked
   */
  void trackDiscard(CardInstance instance, int rounds) {
    if (rounds > 0) {
      entries.add(new Entry(instance, rounds));
    }
  }

  /**
   * Checks whether a specific card instance is currently on cooldown (discarded and waiting to be
   * retrieved), as opposed to sitting in the discard pile for some other reason.
   *
   * @param instance the card instance to check
   * @return true if that exact instance is currently tracked as on cooldown
   */
  boolean isOnCooldown(CardInstance instance) {
    for (Entry entry : entries) {
      if (entry.instance.equals(instance)) {
        return true;
      }
    }
    return false;
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
        if (battleDeck.retrieveInstanceFromDiscard(entry.instance)) {
          retrieved.add(entry.instance.cardId());
        }
        iterator.remove();
      }
    }
    return retrieved;
  }

  private static final class Entry {
    private final CardInstance instance;
    private int roundsRemaining;

    private Entry(CardInstance instance, int roundsRemaining) {
      this.instance = instance;
      this.roundsRemaining = roundsRemaining;
    }
  }
}
