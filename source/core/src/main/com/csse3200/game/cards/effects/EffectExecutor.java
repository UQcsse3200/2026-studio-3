package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.configs.EffectConfig;

/** Executes one validated Team 6 effect configuration against a concrete runtime target. */
public class EffectExecutor {
  /**
   * Applies an effect to a target.
   *
   * <p>Team 6 validates effects while loading card JSON. The checks here protect the runtime from
   * invalid configurations that were manually registered or mutated after loading.
   *
   * @param effect effect configuration to execute
   * @param target runtime target receiving the effect
   * @throws IllegalArgumentException if either argument or the effect contract is invalid
   */
  public void execute(EffectConfig effect, CharacterEffectGateway target) {
    validate(effect, target);

    switch (effect.type) {
      case DAMAGE -> target.damage(effect.value);
      case BLOCK -> target.gainBlock(effect.value);
      case HEAL -> target.heal(effect.value);
      case POISON -> target.applyPoison(effect.value, effect.duration);
      case VULNERABLE -> target.applyVulnerable(effect.value, effect.duration);
      case STRENGTH -> target.gainStrength(effect.value);
    }
  }

  private void validate(EffectConfig effect, CharacterEffectGateway target) {
    if (effect == null) {
      throw new IllegalArgumentException("Effect config cannot be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("Effect target cannot be null");
    }
    if (effect.type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (effect.value <= 0) {
      throw new IllegalArgumentException("Effect value must be positive");
    }
    if (effect.type.usesDuration() && effect.duration <= 0) {
      throw new IllegalArgumentException("Ongoing effect duration must be positive");
    }
    if (!effect.type.usesDuration() && effect.duration != 0) {
      throw new IllegalArgumentException("Instant or combat-long effect duration must be zero");
    }
  }
}
