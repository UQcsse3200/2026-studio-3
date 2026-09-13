package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.CardCatalogGateway;
import java.util.HashSet;
import java.util.Set;

/** In-memory Card Library mock used by Shop integration tests. */
public final class MockCardCatalogGateway implements CardCatalogGateway {
  private final Set<String> cardIds = new HashSet<>();

  public MockCardCatalogGateway(String... cardIds) {
    if (cardIds != null) {
      for (String cardId : cardIds) {
        if (cardId != null) {
          this.cardIds.add(cardId);
        }
      }
    }
  }

  @Override
  public boolean containsCard(String cardId) {
    return cardIds.contains(cardId);
  }
}
