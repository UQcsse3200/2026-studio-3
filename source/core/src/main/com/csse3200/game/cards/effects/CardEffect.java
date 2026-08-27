package com.csse3200.game.cards.effects;

/** One raw effect declared by a card before Team 5 applies combat modifiers. */
public record CardEffect(EffectType type, int value, int duration) {
  public CardEffect {
    if (type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (value <= 0) {
      throw new IllegalArgumentException("Effect value must be positive");
    }
    if (type.usesDuration() && duration <= 0) {
      throw new IllegalArgumentException("Ongoing effect duration must be positive");
    }
    if (!type.usesDuration() && duration != 0) {
      throw new IllegalArgumentException("Instant or combat-long effect duration must be zero");
    }
  }

  public CardEffect(EffectType type, int value) {
    this(type, value, 0);
  }
}
