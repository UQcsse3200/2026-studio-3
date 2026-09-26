package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.DeckGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Controllable persistent-deck mock used by Shop integration tests. */
public final class MockDeckGateway implements DeckGateway {
  private final List<String> cardIds = new ArrayList<>();
  private boolean failAdd;
  private boolean failRemove;
  private String pendingCardId;
  private int pendingCardIndex = -1;

  @Override
  public boolean addCard(String cardId) {
    if (failAdd) {
      return false;
    }
    pendingCardIndex = cardIds.size();
    pendingCardId = cardId;
    cardIds.add(cardId);
    return true;
  }

  @Override
  public boolean removeCard(String cardId) {
    return !failRemove && cardIds.remove(cardId);
  }

  @Override
  public void commitCardAddition(String cardId) {
    if (Objects.equals(pendingCardId, cardId)) {
      clearPendingAddition();
    }
  }

  @Override
  public boolean rollbackCardAddition(String cardId) {
    if (failRemove
        || !Objects.equals(pendingCardId, cardId)
        || pendingCardIndex < 0
        || pendingCardIndex >= cardIds.size()
        || !cardId.equals(cardIds.get(pendingCardIndex))) {
      return false;
    }
    cardIds.remove(pendingCardIndex);
    clearPendingAddition();
    return true;
  }

  public List<String> getCardIds() {
    return Collections.unmodifiableList(cardIds);
  }

  public int getCardCount(String cardId) {
    return Collections.frequency(cardIds, cardId);
  }

  public void setFailAdd(boolean failAdd) {
    this.failAdd = failAdd;
  }

  public void setFailRemove(boolean failRemove) {
    this.failRemove = failRemove;
  }

  public void addExistingCard(String cardId) {
    cardIds.add(cardId);
  }

  private void clearPendingAddition() {
    pendingCardId = null;
    pendingCardIndex = -1;
  }
}
