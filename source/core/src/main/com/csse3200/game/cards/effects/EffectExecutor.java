package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.EffectConfig;

/** Resolves one Team 6 effect config into a Team 5 card effect result. */
public class EffectExecutor {
  /**
   * Resolves one effect from a card.
   *
   * <p>This class does not mutate enemies or the player entity. It only updates Team 5 calculation
   * state where needed, such as strength, and returns a resolved effect record.
   */
  public ResolvedCardEffect resolve(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      PlayerEffectState playerState) {
    validate(cardId, effect, target, sequence, playerState);

    if (target == TargetType.SELF) {
      return resolveSelfEffect(cardId, effect, sequence, playerState);
    }

    return resolveEnemyEffect(cardId, effect, target, sequence, playerState);
  }

  /**
   * Resolves one effect using a read-only combat-state snapshot.
   *
   * <p>This path does not mutate Team 5's internal calculation state. It is intended for the
   * unified card-play flow where Team 7 is the source of truth for player statuses.
   */
  public ResolvedCardEffect resolve(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      CardEffectResolutionContext context) {
    validate(cardId, effect, target, sequence, context);

    if (target == TargetType.SELF) {
      return resolveSelfEffect(cardId, effect, sequence);
    }

    return resolveEnemyEffect(cardId, effect, target, sequence, context);
  }

  private void validate(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      PlayerEffectState playerState) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (effect == null) {
      throw new IllegalArgumentException("Effect config cannot be null");
    }
    if (effect.type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }
    if (sequence < 0) {
      throw new IllegalArgumentException("Effect sequence cannot be negative");
    }
    if (playerState == null) {
      throw new IllegalArgumentException("Player effect state cannot be null");
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

  private void validate(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      CardEffectResolutionContext context) {
    validateBasics(cardId, effect, target, sequence);
    if (context == null) {
      throw new IllegalArgumentException("Card effect resolution context cannot be null");
    }
  }

  private void validateBasics(String cardId, EffectConfig effect, TargetType target, int sequence) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (effect == null) {
      throw new IllegalArgumentException("Effect config cannot be null");
    }
    if (effect.type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }
    if (sequence < 0) {
      throw new IllegalArgumentException("Effect sequence cannot be negative");
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

  private ResolvedCardEffect resolveSelfEffect(
      String cardId, EffectConfig effect, int sequence, PlayerEffectState playerState) {
    if (effect.type == EffectType.STRENGTH) {
      playerState.addStrength(effect.value);
    } else if (effect.type != EffectType.BLOCK && effect.type != EffectType.HEAL) {
      throw new IllegalArgumentException("Unsupported self-targeting effect type: " + effect.type);
    }

    return new ResolvedCardEffect(
        cardId, effect.type, TargetType.SELF, effect.value, effect.duration, sequence);
  }

  private ResolvedCardEffect resolveSelfEffect(String cardId, EffectConfig effect, int sequence) {
    if (effect.type != EffectType.STRENGTH
        && effect.type != EffectType.BLOCK
        && effect.type != EffectType.HEAL) {
      throw new IllegalArgumentException("Unsupported self-targeting effect type: " + effect.type);
    }

    return new ResolvedCardEffect(
        cardId, effect.type, TargetType.SELF, effect.value, effect.duration, sequence);
  }

  private ResolvedCardEffect resolveEnemyEffect(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      PlayerEffectState playerState) {
    if (effect.type == EffectType.DAMAGE) {
      return new ResolvedCardEffect(
          cardId,
          EffectType.DAMAGE,
          target,
          Math.max(0, effect.value + playerState.getStrength()),
          0,
          sequence);
    }

    if (effect.type.usesDuration()) {
      return new ResolvedCardEffect(
          cardId, effect.type, target, effect.value, effect.duration, sequence);
    }

    throw new IllegalArgumentException("Unsupported enemy-targeting effect type: " + effect.type);
  }

  private ResolvedCardEffect resolveEnemyEffect(
      String cardId,
      EffectConfig effect,
      TargetType target,
      int sequence,
      CardEffectResolutionContext context) {
    if (effect.type == EffectType.DAMAGE) {
      return new ResolvedCardEffect(
          cardId, EffectType.DAMAGE, target, context.resolveDamage(effect.value), 0, sequence);
    }

    if (effect.type.usesDuration()) {
      return new ResolvedCardEffect(
          cardId, effect.type, target, effect.value, effect.duration, sequence);
    }

    throw new IllegalArgumentException("Unsupported enemy-targeting effect type: " + effect.type);
  }
}
