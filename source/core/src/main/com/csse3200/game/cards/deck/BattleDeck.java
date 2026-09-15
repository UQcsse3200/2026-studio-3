package com.csse3200.game.cards.deck;

import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Runtime deck state for a single combat encounter.
 *
 * <p>A battle deck is created from the player's long-term deck at battle start, then owns its own
 * draw pile, hand and discard pile. Mutating this class should not change the original {@link
 * PlayerDeck}.
 *
 * <p>Internally every pile stores {@link CardInstance}s rather than bare card IDs, carrying over
 * the unique instance ID each card was given when it entered the {@link PlayerDeck}. Most callers
 * only care about card IDs and can keep using the {@code String}-based methods below, which match
 * the first card with that ID (the same "first match" semantics {@code List.remove(Object)} has
 * always had here). Callers that need to tell duplicate copies of the same card apart — e.g.
 * cooldown tracking, so disabling one "strike" doesn't disable every "strike" — should use the
 * instance-aware methods instead.
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
   * @return drawn card ID, or null if the draw pile is empty
   */
  public String drawOne() {
    CardInstance instance = drawOneInstance();
    return instance == null ? null : instance.cardId();
  }

  private CardInstance drawOneInstance() {
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
    return discardCardInstance(cardId) != null;
  }

  /**
   * Removes the first hand card matching {@code cardId} and moves it to the discard pile, same as
   * {@link #discardCard(String)}, but returns the exact {@link CardInstance} that moved so callers
   * needing per-copy identity (e.g. cooldown tracking) can key off it instead of the shared card
   * ID.
   *
   * @param cardId ID of the card to discard
   * @return the discarded instance, or null if no matching card was in hand
   */
  public CardInstance discardCardInstance(String cardId) {
    CardInstance instance = removeFirstMatching(hand, cardId);
    if (instance == null) {
      return null;
    }
    discardPile.add(instance);
    return instance;
  }

  /**
   * Removes one matching card from the discard pile and moves it back into the hand.
   *
   * @param cardId ID of the card to retrieve
   * @return true if the card was retrieved, otherwise false
   */
  public boolean retrieveFromDiscard(String cardId) {
    CardInstance instance = removeFirstMatching(discardPile, cardId);
    if (instance == null) {
      return false;
    }
    hand.add(instance);
    return true;
  }

  /**
   * Moves a specific card instance from the discard pile back into the hand, by exact identity
   * rather than by matching the first card with a given ID. Used by cooldown tracking so the
   * physical card that was actually discarded is the one retrieved, even when other copies of the
   * same card are also sitting in the discard pile.
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
  public List<String> getDrawPile() {
    return cardIdsOf(drawPile);
  }

  /**
   * @return immutable snapshot of the hand
   */
  public List<String> getHand() {
    return cardIdsOf(hand);
  }

  /**
   * @return immutable snapshot of the discard pile
   */
  public List<String> getDiscardPile() {
    return cardIdsOf(discardPile);
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

  private static CardInstance removeFirstMatching(List<CardInstance> instances, String cardId) {
    if (cardId == null) {
      return null;
    }
    Iterator<CardInstance> iterator = instances.iterator();
    while (iterator.hasNext()) {
      CardInstance instance = iterator.next();
      if (instance.cardId().equals(cardId)) {
        iterator.remove();
        return instance;
      }
    }
    return null;
  }

  private static List<String> cardIdsOf(List<CardInstance> instances) {
    List<String> cardIds = new ArrayList<>(instances.size());
    for (CardInstance instance : instances) {
      cardIds.add(instance.cardId());
    }
    return List.copyOf(cardIds);
  }

  /** Finds an exact owned copy in the current hand. */
  public Optional<CardInstance> getCardInHand(String instanceId) {
    return hand.stream().filter(card -> card.instanceId().equals(instanceId)).findFirst();
  }
}
