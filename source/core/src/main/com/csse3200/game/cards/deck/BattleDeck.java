package com.csse3200.game.cards.deck;

import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Runtime deck state for a single combat encounter.
 *
 * <p>A battle deck is created from the player's long-term deck at battle start, then owns its own
 * draw pile, hand and discard pile. Mutating this class should not change the original {@link
 * PlayerDeck}.
 */
public class BattleDeck {
  private final List<CardInstance> drawPile = new ArrayList<>();
  private final List<CardInstance> hand = new ArrayList<>();
  private final List<CardInstance> discardPile = new ArrayList<>();

  /**
   * Creates battle deck state from a player deck.
   *
   * @param playerDeck source player deck
   */
  public BattleDeck(PlayerDeck playerDeck) {
    if (playerDeck == null) {
      throw new IllegalArgumentException("playerDeck must not be null");
    }
    drawPile.addAll(playerDeck.getCards());
  }

  /** Randomises the current draw pile order. */
  public void shuffleDrawPile() {
    Collections.shuffle(drawPile);
  }

  /**
   * Draws one card from the draw pile into the hand.
   *
   * @return drawn instance, or null if the draw pile is empty
   */
  public CardInstance drawOne() {
    if (drawPile.isEmpty()) {
      reshuffleDiscardIntoDrawPile();
    }

    if (drawPile.isEmpty()) {
      return null;
    }

    CardInstance card = drawPile.remove(0);
    hand.add(card);
    return card;
  }

  /**
   * Draws up to the requested number of cards from the draw pile into the hand.
   *
   * @param count number of cards to draw
   * @return card instances that were drawn, in draw order
   */
  public List<CardInstance> drawCards(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }

    List<CardInstance> drawnCards = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      CardInstance card = drawOne();
      if (card == null) {
        break;
      }
      drawnCards.add(card);
    }
    return List.copyOf(drawnCards);
  }

  /**
   * Moves every card currently in the hand to the discard pile.
   *
   * @return number of cards discarded
   */
  public int discardHand() {
    int discardedCount = hand.size();
    discardPile.addAll(hand);
    hand.clear();
    return discardedCount;
  }

  /**
   * Moves the discard pile into an empty draw pile and shuffles it.
   *
   * @return true if cards were moved and shuffled, otherwise false
   */
  public boolean reshuffleDiscardIntoDrawPile() {
    if (!drawPile.isEmpty() || discardPile.isEmpty()) {
      return false;
    }

    drawPile.addAll(discardPile);
    discardPile.clear();
    shuffleDrawPile();
    return true;
  }

  /**
   * @return immutable snapshot of the draw pile
   */
  public List<CardInstance> getDrawPile() {
    return List.copyOf(drawPile);
  }

  /**
   * @return immutable snapshot of the hand
   */
  public List<CardInstance> getHand() {
    return List.copyOf(hand);
  }

  /**
   * @return immutable snapshot of the discard pile
   */
  public List<CardInstance> getDiscardPile() {
    return List.copyOf(discardPile);
  }

  /**
   * @return number of cards in the draw pile
   */
  public int getDrawPileSize() {
    return drawPile.size();
  }

  /**
   * @return number of cards in the hand
   */
  public int getHandSize() {
    return hand.size();
  }

  /**
   * @return number of cards in the discard pile
   */
  public int getDiscardPileSize() {
    return discardPile.size();
  }

  /**
   * Moves exactly the selected instance from hand to discard. Gameplay must validate and resolve
   * the selected card before calling this operation.
   */
  public boolean playCard(String instanceId) {
    return discardCard(instanceId);
  }

  /** Discards only the matching instanceId; unknown/null IDs do not change any pile. */
  public boolean discardCard(String instanceId) {
    for (int i = 0; i < hand.size(); i++) {
      if (hand.get(i).instanceId().equals(instanceId)) {
        discardPile.add(hand.remove(i));
        return true;
      }
    }
    return false;
  }

  /** Finds an exact owned copy in the current hand. */
  public Optional<CardInstance> getCardInHand(String instanceId) {
    return hand.stream().filter(card -> card.instanceId().equals(instanceId)).findFirst();
  }
}
