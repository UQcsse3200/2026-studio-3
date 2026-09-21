package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.DeckGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Controllable persistent-deck mock used by Shop integration tests. */
public final class MockDeckGateway implements DeckGateway {
  private final List<String> cardIds = new ArrayList<>();
  private final List<PendingAddition> pendingAdditions = new ArrayList<>();
  private boolean failAdd;
  private boolean failRemove;

  @Override
  public boolean addCard(String cardId) {
    if (failAdd) {
      return false;
    }
    pendingAdditions.add(new PendingAddition(cardId, cardIds.size()));
    cardIds.add(cardId);
    return true;
  }

  @Override
  public boolean removeCard(String cardId) {
    return !failRemove && cardIds.remove(cardId);
  }

  @Override
  public void commitCardAddition(String cardId) {
    int pendingIndex = findLatestPendingAddition(cardId);
    if (pendingIndex >= 0) {
      pendingAdditions.remove(pendingIndex);
    }
  }

  @Override
  public boolean rollbackCardAddition(String cardId) {
    int pendingIndex = findLatestPendingAddition(cardId);
    if (failRemove
        || pendingIndex < 0
        || pendingAdditions.get(pendingIndex).cardIndex >= cardIds.size()
        || !cardId.equals(cardIds.get(pendingAdditions.get(pendingIndex).cardIndex))) {
      return false;
    }
    int cardIndex = pendingAdditions.get(pendingIndex).cardIndex;
    cardIds.remove(cardIndex);
    pendingAdditions.remove(pendingIndex);
    for (PendingAddition remaining : pendingAdditions) {
      if (remaining.cardIndex > cardIndex) {
        remaining.cardIndex--;
      }
    }
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
    private int cardIndex;

    private PendingAddition(String cardId, int cardIndex) {
      this.cardId = cardId;
      this.cardIndex = cardIndex;
    }
  }
}
