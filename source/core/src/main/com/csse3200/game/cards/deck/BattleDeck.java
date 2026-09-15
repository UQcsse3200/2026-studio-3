package com.csse3200.game.cards.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime deck state for a single combat encounter.
 *
 * <p>A battle deck is created from the player's long-term deck at battle start, then owns its own
 * draw pile, hand and discard pile. Mutating this class should not change the original {@link
 * PlayerDeck}.
 */
public class BattleDeck {
  private final List<String> drawPile = new ArrayList<>();
  private final List<String> hand = new ArrayList<>();
  private final List<String> discardPile = new ArrayList<>();

  /**
   * Creates battle deck state from a player deck.
   *
   * @param playerDeck source player deck
   */
  public BattleDeck(PlayerDeck playerDeck) {
    if (playerDeck == null) {
      throw new IllegalArgumentException("playerDeck must not be null");
    }
    drawPile.addAll(playerDeck.getCardIds());
  }

  /** Randomises the current draw pile order. */
  public void shuffleDrawPile() {
    Collections.shuffle(drawPile);
  }

  /**
   * Draws one card from the draw pile into the hand.
   *
   * @return drawn card ID, or null if the draw pile is empty
   */
  public String drawOne() {
    if (drawPile.isEmpty()) {
      reshuffleDiscardIntoDrawPile();
    }

    if (drawPile.isEmpty()) {
      return null;
    }

    String cardId = drawPile.remove(0);
    hand.add(cardId);
    return cardId;
  }

  /**
   * Draws up to the requested number of cards from the draw pile into the hand.
   *
   * @param count number of cards to draw
   * @return card IDs that were drawn, in draw order
   */
  public List<String> drawCards(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }

    List<String> drawnCards = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String cardId = drawOne();
      if (cardId == null) {
        break;
      }
      drawnCards.add(cardId);
    }
    return List.copyOf(drawnCards);
  }

  /**
   * Plays a card from the hand and moves it to the discard pile.
   *
   * <p>Card validation and effect resolution should be completed before this method is called.
   *
   * @param cardId ID of the card being played
   * @return true if the card was moved, otherwise false
   */
  public boolean playCard(String cardId) {
    return discardCard(cardId);
  }

  /**
   * Removes one matching card from the hand and moves it to the discard pile.
   *
   * @param cardId ID of the card to discard
   * @return true if the card was discarded, otherwise false
   */
  public boolean discardCard(String cardId) {
    if (cardId == null || !hand.remove(cardId)) {
      return false;
    }

    discardPile.add(cardId);
    return true;
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
  public List<String> getDrawPile() {
    return List.copyOf(drawPile);
  }

  /**
   * @return immutable snapshot of the hand
   */
  public List<String> getHand() {
    return List.copyOf(hand);
  }

  /**
   * @return immutable snapshot of the discard pile
   */
  public List<String> getDiscardPile() {
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
}
