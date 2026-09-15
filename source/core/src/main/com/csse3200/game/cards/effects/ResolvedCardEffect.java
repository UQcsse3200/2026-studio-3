package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;

/** One card effect after Team 5 has applied card resolution rules and player-side modifiers. */
public record ResolvedCardEffect(
    String cardId, EffectType type, TargetType target, int value, int duration, int sequence) {
  public ResolvedCardEffect {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }
    if (value < 0) {
      throw new IllegalArgumentException("Resolved effect value cannot be negative");
    }
    if (duration < 0) {
      throw new IllegalArgumentException("Resolved effect duration cannot be negative");
    }
    if (sequence < 0) {
      throw new IllegalArgumentException("Resolved effect sequence cannot be negative");
    }
  }
}
