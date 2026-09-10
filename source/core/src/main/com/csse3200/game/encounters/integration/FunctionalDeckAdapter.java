package com.csse3200.game.encounters.integration;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Adapts add/remove functions to the Team 2 deck boundary.
 *
 * <p>After Team 5 is merged, the add function can call {@code PlayerDeck.addCard(cardId)} and the
 * remove function can call {@code PlayerDeck.removeCard(cardId)}.
 */
public final class FunctionalDeckAdapter implements DeckGateway {
  private final Predicate<String> addCard;
  private final Predicate<String> removeCard;

  /**
   * Creates an adapter around add-card and remove-card functions.
   *
   * @param addCard function that adds one card and reports success
   * @param removeCard function that removes one card and reports success
   */
  public FunctionalDeckAdapter(Predicate<String> addCard, Predicate<String> removeCard) {
    this.addCard = Objects.requireNonNull(addCard, "addCard cannot be null");
    this.removeCard = Objects.requireNonNull(removeCard, "removeCard cannot be null");
  }

  @Override
  public boolean addCard(String cardId) {
    return cardId != null && !cardId.isBlank() && addCard.test(cardId);
  }

  @Override
  public boolean removeCard(String cardId) {
    return cardId != null && !cardId.isBlank() && removeCard.test(cardId);
  }
}
