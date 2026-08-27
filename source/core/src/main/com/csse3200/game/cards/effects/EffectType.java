package com.csse3200.game.cards.effects;

/** Effect kinds that Team 5 can resolve during combat. */
public enum EffectType {
  DAMAGE,
  BLOCK,
  HEAL,
  POISON,
  VULNERABLE,
  STRENGTH;

  /** Whether this effect needs a positive turn duration. */
  public boolean usesDuration() {
    return this == POISON || this == VULNERABLE;
  }
}
