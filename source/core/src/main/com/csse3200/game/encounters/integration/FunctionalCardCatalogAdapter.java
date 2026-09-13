package com.csse3200.game.encounters.integration;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Adapts a card lookup function to {@link CardCatalogGateway}.
 *
 * <p>After Team 6 is merged, this can be constructed with {@code cardId ->
 * cardService.getCard(cardId).isPresent()} without coupling Team 2 code to the Card Library's
 * concrete storage class.
 */
public final class FunctionalCardCatalogAdapter implements CardCatalogGateway {
  private final Predicate<String> cardLookup;

  /**
   * Creates an adapter around a card-existence lookup function.
   *
   * @param cardLookup function returning whether a card ID is registered
   */
  public FunctionalCardCatalogAdapter(Predicate<String> cardLookup) {
    this.cardLookup = Objects.requireNonNull(cardLookup, "cardLookup cannot be null");
  }

  @Override
  public boolean containsCard(String cardId) {
    return cardId != null && !cardId.isBlank() && cardLookup.test(cardId);
  }
}
