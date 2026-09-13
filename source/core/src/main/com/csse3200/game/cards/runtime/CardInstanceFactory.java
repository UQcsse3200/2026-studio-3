package com.csse3200.game.cards.runtime;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.Optional;
import java.util.UUID;

/** Creates uniquely identified runtime card copies from registered card definitions. */
public final class CardInstanceFactory {
  private final CardService cardService;

  /**
   * @param cardService registry used to verify requested card IDs
   */
  public CardInstanceFactory(CardService cardService) {
    if (cardService == null) {
      throw new IllegalArgumentException("cardService must not be null");
    }
    this.cardService = cardService;
  }

  /**
   * Creates a normal card copy with a new stable instance ID.
   *
   * @param cardId registered shared card ID
   * @return a normal runtime instance
   */
  public CardInstance create(String cardId) {
    requireRegisteredCard(cardId, false);
    return new CardInstance(newInstanceId(), cardId, CardInstance.BASE_LEVEL);
  }

  /**
   * Creates an already-upgraded card copy for tests, debugging, or explicit upgraded rewards.
   *
   * @param cardId registered shared card ID with an upgrade definition
   * @return an upgraded runtime instance
   */
  public CardInstance createUpgraded(String cardId) {
    requireRegisteredCard(cardId, true);
    return new CardInstance(newInstanceId(), cardId, CardInstance.UPGRADED_LEVEL);
  }

  private CardConfig requireRegisteredCard(String cardId, boolean requireUpgrade) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("cardId must not be null or blank");
    }

    Optional<CardConfig> config = cardService.getCard(cardId);
    if (config == null || config.isEmpty()) {
      throw new IllegalArgumentException("Unknown card ID: " + cardId);
    }
    if (requireUpgrade && config.get().upgrade == null) {
      throw new IllegalArgumentException("Card has no upgrade definition: " + cardId);
    }
    return config.get();
  }

  private static String newInstanceId() {
    return UUID.randomUUID().toString();
  }
}
