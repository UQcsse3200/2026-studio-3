package com.csse3200.game.cards.effects;

/** Effect result that Team 5 can hand to enemy, combat, animation, or logging systems. */
public record ResolvedCardEffect(
    String cardId, EffectType type, TargetType target, int value, int duration) {
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
  }
}
