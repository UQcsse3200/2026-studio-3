package com.csse3200.game.cards;

/**
 * The kinds of effect a card can apply. Each constant records whether the effect lasts over
 * multiple turns, which determines how the duration field of an {@link
 * com.csse3200.game.cards.configs.EffectConfig} is interpreted.
 */
public enum EffectType {
  /** Immediate damage dealt to the target. */
  DAMAGE(false),
  /** Immediate block granted to the target. */
  BLOCK(false),
  /** Immediate health restored to the target. */
  HEAL(false),
  /** Damage over time applied at the start of the target's turn. */
  POISON(true),
  /** Increases damage the target receives while active. */
  VULNERABLE(true),
  /** Increases damage the target deals for the rest of the combat. */
  STRENGTH(false);

  private final boolean usesDuration;

  EffectType(boolean usesDuration) {
    this.usesDuration = usesDuration;
  }

  /**
   * @return true if this effect requires a positive duration, false if it applies immediately or
   *     lasts for the whole combat
   */
  public boolean usesDuration() {
    return usesDuration;
  }
}
