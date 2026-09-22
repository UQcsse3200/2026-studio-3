package com.csse3200.game.encounters.integration;

import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Connects Team 2 shop transactions to Team 5's persistent player deck. */
public final class PlayerDeckAdapter implements DeckGateway {
  private final PlayerDeck playerDeck;
  private final List<PendingAddition> pendingAdditions = new ArrayList<>();

  /**
   * Creates a deck boundary backed by Team 5's player deck.
   *
   * @param playerDeck persistent deck that receives purchased cards
   */
  public PlayerDeckAdapter(PlayerDeck playerDeck) {
    this.playerDeck = Objects.requireNonNull(playerDeck, "playerDeck cannot be null");
  }

  @Override
  public synchronized boolean canAddCard(String cardId) {
    return playerDeck.canAddCard(cardId);
  }

  @Override
  public synchronized boolean addCard(String cardId) {
    if (!canAddCard(cardId)) {
      return false;
    }

    int insertionIndex = playerDeck.size();
    try {
      playerDeck.addCard(cardId);
    } catch (IllegalArgumentException exception) {
      return false;
    }
    String instanceId = playerDeck.getCards().get(insertionIndex).instanceId();
    pendingAdditions.add(new PendingAddition(cardId, instanceId, insertionIndex));
    return true;
  }

  @Override
  public synchronized boolean removeCard(String cardId) {
    try {
      return playerDeck.getCards().stream()
          .filter(card -> card.cardId().equals(cardId))
          .findFirst()
          .map(CardInstance::instanceId)
          .map(playerDeck::removeCard)
          .orElse(false);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  @Override
  public synchronized void commitCardAddition(String cardId) {
    int pendingIndex = findLatestPendingAddition(cardId);
    if (pendingIndex >= 0) {
      pendingAdditions.remove(pendingIndex);
    }
  }

  @Override
  public synchronized boolean rollbackCardAddition(String cardId) {
    int pendingIndex = findLatestPendingAddition(cardId);
    if (pendingIndex < 0) {
      return false;
    }
    PendingAddition pending = pendingAdditions.get(pendingIndex);

    List<CardInstance> cards = playerDeck.getCards();
    if (pending.cardIndex >= cards.size()) {
      return false;
    }

    // Verify the exact instance is still at the expected position before removing it, so a
    // duplicate card of the same ID that has shifted into this slot is never rolled back by
    // mistake.
    CardInstance pendingCard = cards.get(pending.cardIndex);
    if (!Objects.equals(cardId, pendingCard.cardId())
        || !Objects.equals(pending.instanceId, pendingCard.instanceId())) {
      return false;
    }

    playerDeck.removeCardAt(pending.cardIndex);
    pendingAdditions.remove(pendingIndex);
    for (PendingAddition remaining : pendingAdditions) {
      if (remaining.cardIndex > pending.cardIndex) {
        remaining.cardIndex--;
      }
    }
    return true;
  }

  private int findLatestPendingAddition(String cardId) {
    for (int index = pendingAdditions.size() - 1; index >= 0; index--) {
      if (Objects.equals(pendingAdditions.get(index).cardId, cardId)) {
        return index;
      }
    }
    return -1;
  }

  private static final class PendingAddition {
    private final String cardId;
    private final String instanceId;
    private int cardIndex;

    private PendingAddition(String cardId, String instanceId, int cardIndex) {
      this.cardId = cardId;
      this.instanceId = instanceId;
      this.cardIndex = cardIndex;
    }
  }
}
