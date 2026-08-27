package com.csse3200.game.cards.configs;

import com.csse3200.game.cards.EffectType;

/**
 * A single effect applied by a card, loaded as part of {@code configs/cards.json}. The meaning of
 * value depends on the effect type: it is a damage amount for DAMAGE, a block amount for BLOCK, an
 * amount of health for HEAL, and a number of stacks for POISON, VULNERABLE and STRENGTH. Duration
 * is only used by effects where {@link EffectType#usesDuration()} is true.
 */
public class EffectConfig {
  /** The kind of effect applied. */
  public EffectType type = EffectType.DAMAGE;

  /** Magnitude of the effect, interpreted according to the effect type. */
  public int value = 0;

  /** Number of turns the effect lasts, unused by instant and combat-long effects. */
  public int duration = 0;

  /** Required by the JSON deserialiser. */
  public EffectConfig() {}

  /**
   * Creates an effect with an explicit duration.
   *
   * @param type the kind of effect
   * @param value the magnitude of the effect
   * @param duration the number of turns the effect lasts
   */
  public EffectConfig(EffectType type, int value, int duration) {
    this.type = type;
    this.value = value;
    this.duration = duration;
  }

  /**
   * Creates an effect with no duration, for instant or combat-long effects.
   *
   * @param type the kind of effect
   * @param value the magnitude of the effect
   */
  public EffectConfig(EffectType type, int value) {
    this(type, value, 0);
  }
}
