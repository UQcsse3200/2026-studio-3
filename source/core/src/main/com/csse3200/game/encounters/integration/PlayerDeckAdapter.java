package com.csse3200.game.encounters.integration;

import com.csse3200.game.cards.deck.PlayerDeck;
import java.util.List;
import java.util.Objects;

/** Connects Team 2 shop transactions to Team 5's persistent player deck. */
public final class PlayerDeckAdapter implements DeckGateway {
  private final PlayerDeck playerDeck;
  private String pendingCardId;
  private int pendingCardIndex = -1;

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
    pendingCardId = cardId;
    pendingCardIndex = insertionIndex;
    return true;
  }

  @Override
  public synchronized boolean removeCard(String cardId) {
    try {
      return playerDeck.removeCard(cardId);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  @Override
  public synchronized void commitCardAddition(String cardId) {
    if (Objects.equals(pendingCardId, cardId)) {
      clearPendingAddition();
    }
  }

  @Override
  public synchronized boolean rollbackCardAddition(String cardId) {
    if (!Objects.equals(pendingCardId, cardId) || pendingCardIndex < 0) {
      return false;
    }

    List<String> cardIds = playerDeck.getCardIds();
    if (pendingCardIndex >= cardIds.size() || !cardId.equals(cardIds.get(pendingCardIndex))) {
      return false;
    }

    playerDeck.removeCardAt(pendingCardIndex);
    clearPendingAddition();
    return true;
  }

  private void clearPendingAddition() {
    pendingCardId = null;
    pendingCardIndex = -1;
  }
}
