package com.csse3200.game.cards.effects;

import java.util.List;

/** Minimal Team 5 input after a card definition has been converted from JSON or another source. */
public record CardEffectRequest(String cardId, TargetType target, List<CardEffect> effects) {
  public CardEffectRequest {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }
    if (effects == null || effects.isEmpty()) {
      throw new IllegalArgumentException("Card must contain at least one effect");
    }
    try {
      effects = List.copyOf(effects);
    } catch (NullPointerException exception) {
      throw new IllegalArgumentException("Card effects cannot contain null", exception);
    }
  }
}
