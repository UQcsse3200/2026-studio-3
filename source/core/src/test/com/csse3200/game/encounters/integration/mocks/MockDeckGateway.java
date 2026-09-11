package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.DeckGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Controllable persistent-deck mock used by Shop integration tests. */
public final class MockDeckGateway implements DeckGateway {
  private final List<String> cardIds = new ArrayList<>();
  private boolean failAdd;
  private boolean failRemove;

  @Override
  public boolean addCard(String cardId) {
    if (failAdd) {
      return false;
    }
    cardIds.add(cardId);
    return true;
  }

  @Override
  public boolean removeCard(String cardId) {
    return !failRemove && cardIds.remove(cardId);
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
}
