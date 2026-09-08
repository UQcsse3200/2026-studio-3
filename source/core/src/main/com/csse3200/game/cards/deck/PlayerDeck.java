package com.csse3200.game.cards.deck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stores the cards owned by the player outside a single combat encounter.
 *
 * <p>The deck stores Team 6 card IDs instead of full card definitions. Card data, costs, targets
 * and effects should be resolved through the card library when another system needs the full card
 * configuration. This keeps the player deck independent from combat-only state such as draw pile,
 * hand and discard pile.
 */
public class PlayerDeck {
  private final List<String> cardIds = new ArrayList<>();

  /** Creates an empty player deck. */
  public PlayerDeck() {}

  /**
   * Creates a player deck containing the given card IDs in order.
   *
   * @param cardIds card IDs to add to the deck
   */
  public PlayerDeck(Collection<String> cardIds) {
    addCards(cardIds);
  }

  /**
   * Adds one card to the end of the deck.
   *
   * @param cardId Team 6 card ID
   * @throws IllegalArgumentException if the card ID is not registered
   */
  public void addCard(String cardId) {
    if (!canAddCard(cardId)) {
      throw new IllegalArgumentException("cardId must be registered");
    }
    cardIds.add(cardId);
  }

  /**
   * Checks whether a card may be added to this player deck.
   *
   * <p>Sprint 1 allows duplicate cards and has no deck-size limit, so this check only verifies that
   * the card ID is registered. This method does not modify the deck.
   *
   * @param cardId card ID to check
   * @return true if the card can be added, otherwise false
   */
  public boolean canAddCard(String cardId) {
    return CardIdRegistry.isRegistered(cardId);
  }

  /**
   * Adds every provided card ID to the deck in iteration order.
   *
   * @param cardIds card IDs to add
   */
  public void addCards(Collection<String> cardIds) {
    if (cardIds == null) {
      throw new IllegalArgumentException("cardIds must not be null");
    }
    for (String cardId : cardIds) {
      addCard(cardId);
    }
  }

  /**
   * Removes the first matching card ID from the deck.
   *
   * @param cardId card ID to remove
   * @return true if a card was removed, false if the deck did not contain that card
   */
  public boolean removeCard(String cardId) {
    return cardIds.remove(validateCardId(cardId));
  }

  /**
   * Removes the card at a specific deck position.
   *
   * @param index card position
   * @return the removed card ID
   */
  public String removeCardAt(int index) {
    return cardIds.remove(index);
  }

  /**
   * Checks whether the deck contains at least one copy of a card.
   *
   * @param cardId card ID to search for
   * @return true if the deck contains the card
   */
  public boolean contains(String cardId) {
    return cardIds.contains(validateCardId(cardId));
  }

  /**
   * Counts how many copies of a card are in the deck.
   *
   * @param cardId card ID to count
   * @return number of matching cards
   */
  public int count(String cardId) {
    String validCardId = validateCardId(cardId);
    int count = 0;
    for (String existingCardId : cardIds) {
      if (existingCardId.equals(validCardId)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns a snapshot of card IDs in deck order.
   *
   * @return immutable list of card IDs
   */
  public List<String> getCardIds() {
    return List.copyOf(cardIds);
  }

  /**
   * Creates an independent copy of this player deck.
   *
   * @return copied player deck
   */
  public PlayerDeck copy() {
    return new PlayerDeck(cardIds);
  }

  /** Removes all cards from the deck. */
  public void clear() {
    cardIds.clear();
  }

  /**
   * @return number of cards in the deck, including duplicate copies
   */
  public int size() {
    return cardIds.size();
  }

  /**
   * @return true if the deck has no cards
   */
  public boolean isEmpty() {
    return cardIds.isEmpty();
  }

  private static String validateCardId(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("cardId must not be null or blank");
    }
    return cardId;
  }
}
