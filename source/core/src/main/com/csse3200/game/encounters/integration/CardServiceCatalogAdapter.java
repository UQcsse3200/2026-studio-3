package com.csse3200.game.encounters.integration;

import com.csse3200.game.cards.CardService;
import java.util.Objects;

/** Connects Team 2 shop validation to Team 6's production card service. */
public final class CardServiceCatalogAdapter implements CardCatalogGateway {
  private final CardService cardService;

  /**
   * Creates a card-catalog boundary backed by Team 6's shared service.
   *
   * @param cardService production card lookup service
   */
  public CardServiceCatalogAdapter(CardService cardService) {
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
  }

  @Override
  public boolean containsCard(String cardId) {
    return cardId != null && !cardId.isBlank() && cardService.getCard(cardId).isPresent();
  }
}
