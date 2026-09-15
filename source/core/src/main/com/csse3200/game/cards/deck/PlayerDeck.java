package com.csse3200.game.cards.deck;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
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
 *
 * <p>Internally each card is tracked as a {@link CardInstance}: a card ID plus a unique instance
 * ID generated the moment it is added, so duplicate copies of the same card (e.g. two "strike"
 * cards) can be told apart by callers that need to. Most callers only care about card IDs and can
 * keep using the {@code String}-based methods below; {@link #getCards()} exposes the instances.
 */
public class PlayerDeck {
  private final CardService cardService;
  private final List<CardInstance> cards = new ArrayList<>();

  /** Creates an empty player deck backed by the configured card definitions. */
  public PlayerDeck() {
    this(loadDefaultCardService());
  }

  /**
   * Creates an empty player deck using the supplied source of card definitions.
   *
   * @param cardService authoritative card lookup service
   */
  public PlayerDeck(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("cardService must not be null");
    }
    this.cardService = cardService;
  }

  /**
   * Creates a player deck containing the given card IDs in order.
   *
   * @param cardIds card IDs to add to the deck
   */
  public PlayerDeck(Collection<String> cardIds) {
    this(loadDefaultCardService(), cardIds);
  }

  /**
   * Creates a player deck containing the given card IDs in order, validated by the supplied card
   * service.
   *
   * @param cardService authoritative card lookup service
   * @param cardIds card IDs to add to the deck
   */
  public PlayerDeck(CardService cardService, Collection<String> cardIds) {
    this(cardService);
    addCards(cardIds);
  }

  /**
   * Adds one card to the end of the deck, generating a fresh unique instance ID for it.
   *
   * @param cardId Team 6 card ID
   * @throws IllegalArgumentException if the card ID is not registered
   */
  public void addCard(String cardId) {
    if (!canAddCard(cardId)) {
      throw new IllegalArgumentException("cardId must be registered");
    }
    cards.add(CardInstance.of(cardId));
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
    return cardService.getCard(cardId).isPresent();
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
    String validCardId = validateCardId(cardId);
    for (int i = 0; i < cards.size(); i++) {
      if (cards.get(i).cardId().equals(validCardId)) {
        cards.remove(i);
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the card at a specific deck position.
   *
   * @param index card position
   * @return the removed card ID
   */
  public String removeCardAt(int index) {
    return cards.remove(index).cardId();
  }

  /**
   * Checks whether the deck contains at least one copy of a card.
   *
   * @param cardId card ID to search for
   * @return true if the deck contains the card
   */
  public boolean contains(String cardId) {
    String validCardId = validateCardId(cardId);
    for (CardInstance card : cards) {
      if (card.cardId().equals(validCardId)) {
        return true;
      }
    }
    return false;
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
    for (CardInstance card : cards) {
      if (card.cardId().equals(validCardId)) {
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
    List<String> cardIds = new ArrayList<>(cards.size());
    for (CardInstance card : cards) {
      cardIds.add(card.cardId());
    }
    return List.copyOf(cardIds);
  }

  /**
   * Returns a snapshot of every card in the deck as a distinct {@link CardInstance}, in deck
   * order. Unlike {@link #getCardIds()}, this lets callers tell duplicate copies of the same card
   * apart.
   *
   * @return immutable list of card instances
   */
  public List<CardInstance> getCards() {
    return List.copyOf(cards);
  }

  /**
   * Creates an independent copy of this player deck.
   *
   * <p>The copy's cards get freshly generated instance IDs — it is a separate deck, not a shared
   * view of the same physical cards.
   *
   * @return copied player deck
   */
  public PlayerDeck copy() {
    return new PlayerDeck(cardService, getCardIds());
  }

  /** Removes all cards from the deck. */
  public void clear() {
    cards.clear();
  }

  /**
   * @return number of cards in the deck, including duplicate copies
   */
  public int size() {
    return cards.size();
  }

  /**
   * @return true if the deck has no cards
   */
  public boolean isEmpty() {
    return cards.isEmpty();
  }

  private static String validateCardId(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("cardId must not be null or blank");
    }
    return cardId;
  }

  private static CardService loadDefaultCardService() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }
}
