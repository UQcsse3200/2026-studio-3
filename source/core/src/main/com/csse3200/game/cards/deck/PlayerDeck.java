package com.csse3200.game.cards.deck;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardInstanceFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stores the cards owned by the player outside a single combat encounter.
 *
 * <p>The deck stores Team 6 card IDs instead of full card definitions. Card data, costs, targets
 * and effects should be resolved through the card library when another system needs the full card
 * configuration. This keeps the player deck independent from combat-only state such as draw pile,
 * hand and discard pile.
 */
public class PlayerDeck {
  private final CardService cardService;
  private final CardInstanceFactory instanceFactory;
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
    this.instanceFactory = new CardInstanceFactory(cardService);
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
   * Restores existing instances without regenerating identity. A named factory avoids Java's
   * generic erasure clash with the existing Collection&lt;String&gt; constructor.
   */
  public static PlayerDeck fromInstances(CardService service, Collection<CardInstance> cards) {
    if (cards == null) {
      throw new IllegalArgumentException("cards must not be null");
    }
    PlayerDeck deck = new PlayerDeck(service);
    for (CardInstance card : cards) {
      deck.addCard(card);
    }
    return deck;
  }

  /** Adds a registered copy; duplicate instance IDs are rejected across all definitions. */
  public void addCard(CardInstance card) {
    if (card == null) {
      throw new IllegalArgumentException("card must not be null");
    }
    var config =
        cardService
            .getCard(card.cardId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + card.cardId()));
    if (card.isUpgraded() && config.upgrade == null) {
      throw new IllegalArgumentException("Card has no upgrade definition: " + card.cardId());
    }
    if (containsInstance(card.instanceId())) {
      throw new IllegalArgumentException("Duplicate instance ID: " + card.instanceId());
    }
    cards.add(card);
  }

  /**
   * @deprecated Acquisition callers should explicitly create and add a CardInstance.
   */
  @Deprecated
  public void addCard(String cardId) {
    addCard(instanceFactory.create(cardId));
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
    return cardId != null && !cardId.isBlank() && cardService.getCard(cardId).isPresent();
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
      addCard(instanceFactory.create(cardId));
    }
  }

  /** Removes exactly the selected instance; never falls back to a definition ID. */
  public boolean removeCard(String instanceId) {
    validateId(instanceId);
    return cards.removeIf(card -> card.instanceId().equals(instanceId));
  }

  /** Removes and returns the copy at the given position. */
  public CardInstance removeCardAt(int index) {
    return cards.remove(index);
  }

  /** Returns the copy with this instanceId, or empty if it is not owned. */
  public Optional<CardInstance> getCard(String instanceId) {
    validateId(instanceId);
    return cards.stream().filter(card -> card.instanceId().equals(instanceId)).findFirst();
  }

  /** Checks ownership of an exact copy. */
  public boolean containsInstance(String instanceId) {
    return getCard(instanceId).isPresent();
  }

  /** Counts copies of a shared definition, irrespective of upgrade level. */
  public int countByCardId(String cardId) {
    validateId(cardId);
    return (int) cards.stream().filter(card -> card.cardId().equals(cardId)).count();
  }

  /**
   * Replaces only the selected owned copy with its upgraded value, in the same slot.
   * Eligibility/currency/UI belong to the upgrade feature; this operation only updates deck state.
   * Unknown IDs, missing upgrade definitions and already-upgraded copies fail without mutation.
   */
  public CardInstance upgradeCard(String instanceId) {
    CardInstance card =
        getCard(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown instance ID: " + instanceId));
    var config =
        cardService
            .getCard(card.cardId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + card.cardId()));
    if (config.upgrade == null) {
      throw new IllegalArgumentException("Card has no upgrade definition: " + card.cardId());
    }
    CardInstance upgraded = card.upgrade();
    cards.set(cards.indexOf(card), upgraded);
    return upgraded;
  }

  /** Returns an immutable snapshot preserving identity, order and upgrade level. */
  public List<CardInstance> getCards() {
    return List.copyOf(cards);
  }

  /**
   * @deprecated Definition-only projection loses instance identity and upgrade state.
   */
  @Deprecated
  public List<String> getCardIds() {
    return cards.stream().map(CardInstance::cardId).toList();
  }

  /**
   * @deprecated Use countByCardId to make definition-level intent explicit.
   */
  @Deprecated
  public int count(String cardId) {
    return countByCardId(cardId);
  }

  /**
   * @deprecated Definition-level query; use containsInstance for an exact owned copy.
   */
  @Deprecated
  public boolean contains(String cardId) {
    return countByCardId(cardId) > 0;
  }

  /**
   * Creates an independent copy of this player deck.
   *
   * @return copied player deck
   */
  public PlayerDeck copy() {
    return fromInstances(cardService, cards);
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

  private static void validateId(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("ID must not be null or blank");
    }
  }

  private static CardService loadDefaultCardService() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }
}
