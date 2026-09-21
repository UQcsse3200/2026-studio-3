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
 *
 * <p>Every pile stores {@link CardInstance}s and preserves the identity assigned when a card
 * entered the {@link PlayerDeck}. All string parameters in this class are exact instance IDs;
 * definition IDs are never used to choose one copy from a pile.
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
   * @return drawn card instance, or null if the draw pile is empty
   */
  public CardInstance drawOne() {
    if (drawPile.isEmpty()) {
      reshuffleDiscardIntoDrawPile();
    }

    if (drawPile.isEmpty()) {
      return null;
    }

    CardInstance instance = drawPile.removeFirst();
    hand.add(instance);
    return instance;
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
   * Plays a card from the hand and moves it to the discard pile.
   *
   * <p>Card validation and effect resolution should be completed before this method is called.
   *
   * @param instanceId ID of the card being played
   * @return true if the card was moved, otherwise false
   */
  public boolean playCard(String instanceId) {
    return discardCard(instanceId);
  }

  /**
   * Removes the exact card from the hand and moves it to the discard pile.
   *
   * @param instanceId ID of the card to discard
   * @return true if the card was discarded, otherwise false
   */
  public boolean discardCard(String instanceId) {
    return discardCardInstance(instanceId) != null;
  }

  /**
   * Removes the hand card with {@code instanceId} and moves it to the discard pile, same as {@link
   * #discardCard(String)}, but returns the exact {@link CardInstance} that moved.
   *
   * @param instanceId ID of the card to discard
   * @return the discarded instance, or null if no matching card was in hand
   */
  public CardInstance discardCardInstance(String instanceId) {
    CardInstance instance = removeByInstanceId(hand, instanceId);
    if (instance == null) {
      return null;
    }
    discardPile.add(instance);
    return instance;
  }

  /**
   * Removes one matching card from the discard pile and moves it back into the hand.
   *
   * @param instanceId ID of the card to retrieve
   * @return true if the card was retrieved, otherwise false
   */
  public boolean retrieveFromDiscard(String instanceId) {
    CardInstance instance = removeByInstanceId(discardPile, instanceId);
    if (instance == null) {
      return false;
    }
    hand.add(instance);
    return true;
  }

  /**
   * Moves a specific card instance from the discard pile back into the hand. Used by cooldown
   * tracking so the physical card that was discarded is the one retrieved.
   *
   * @param instance the exact instance to retrieve
   * @return true if that instance was in the discard pile and was moved, otherwise false
   */
  public boolean retrieveInstanceFromDiscard(CardInstance instance) {
    if (instance == null || !discardPile.remove(instance)) {
      return false;
    }
    hand.add(instance);
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
  public List<CardInstance> getDrawPile() {
    return List.copyOf(drawPile);
  }

  /**
   * @return immutable snapshot of the hand as distinct card instances, letting callers tell
   *     duplicate copies of the same card apart
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
   * @return immutable snapshot of the hand as distinct card instances, letting callers tell
   *     duplicate copies of the same card apart
   */
  public List<CardInstance> getHandInstances() {
    return List.copyOf(hand);
  }

  /**
   * @return immutable snapshot of the discard pile as distinct card instances
   */
  public List<CardInstance> getDiscardPileInstances() {
    return List.copyOf(discardPile);
  }

  /**
   * @return immutable snapshot of the draw pile as distinct card instances
   */
  public List<CardInstance> getDrawPileInstances() {
    return List.copyOf(drawPile);
  }

  /**
   * @return immutable snapshot of every card in this battle deck (draw pile, hand and discard pile
   *     combined), as distinct instances. Used by the deck-rearrange UI to offer the player's whole
   *     pool, regardless of which pile a given copy currently sits in.
   */
  public List<CardInstance> getAllInstances() {
    List<CardInstance> all = new ArrayList<>(drawPile.size() + hand.size() + discardPile.size());
    all.addAll(drawPile);
    all.addAll(hand);
    all.addAll(discardPile);
    return List.copyOf(all);
  }

  /**
   * Replaces the current hand with an exact set of card instances, by identity rather than card ID.
   * The discard pile is left completely untouched — so any cooldowns in progress keep counting down
   * normally — and every other instance (the old hand plus the draw pile) is folded back into the
   * draw pile and reshuffled.
   *
   * @param newHand the exact card instances that should make up the new hand
   * @throws IllegalArgumentException if {@code newHand} is null, or contains an instance that isn't
   *     currently available (i.e. isn't in this battle deck's hand or draw pile — most commonly
   *     because it's on cooldown in the discard pile)
   */
  public void setHandInstances(List<CardInstance> newHand) {
    if (newHand == null) {
      throw new IllegalArgumentException("newHand must not be null");
    }

    List<CardInstance> remainingAvailable = new ArrayList<>(hand);
    remainingAvailable.addAll(drawPile);

    List<CardInstance> resolvedHand = new ArrayList<>(newHand.size());
    for (CardInstance instance : newHand) {
      if (instance == null || !remainingAvailable.remove(instance)) {
        throw new IllegalArgumentException(
            "Card instance is not available to place in hand (missing, duplicated in the "
                + "request, or on cooldown in the discard pile): "
                + instance);
      }
      resolvedHand.add(instance);
    }

    hand.clear();
    hand.addAll(resolvedHand);
    drawPile.clear();
    drawPile.addAll(remainingAvailable);
    shuffleDrawPile();
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

  private static CardInstance removeByInstanceId(List<CardInstance> instances, String instanceId) {
    if (instanceId == null) {
      return null;
    }
    for (int i = 0; i < instances.size(); i++) {
      CardInstance instance = instances.get(i);
      if (instance.instanceId().equals(instanceId)) {
        instances.remove(i);
        return instance;
      }
    }
    return null;
  }

  /** Finds an exact owned copy in the current hand. */
  public Optional<CardInstance> getCardInHand(String instanceId) {
    return hand.stream().filter(card -> card.instanceId().equals(instanceId)).findFirst();
  }
}
