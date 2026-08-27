package com.csse3200.game.cards.effects;

import java.util.List;

/** Resolves one card effect according to Team 5's combat boundary. */
public class EffectExecutor {
  /**
   * Resolves one raw card effect.
   *
   * <p>Enemy effects are returned for the combat/enemy systems to apply. Player self effects are
   * sent through {@link CharacterEffectGateway}, because the player stats system owns the actual
   * player state.
   *
   * @return outgoing effects that still need to be applied by another system
   */
  public List<ResolvedCardEffect> execute(
      String cardId, CardEffect effect, TargetType target, CharacterEffectGateway playerStats) {
    validate(cardId, effect, target, playerStats);

    if (target == TargetType.SELF) {
      applySelfEffect(effect, playerStats);
      return List.of();
    }

    return List.of(resolveEnemyEffect(cardId, effect, target, playerStats));
  }

  private void validate(
      String cardId, CardEffect effect, TargetType target, CharacterEffectGateway playerStats) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (effect == null) {
      throw new IllegalArgumentException("Effect cannot be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }
    if (playerStats == null) {
      throw new IllegalArgumentException("Player stats gateway cannot be null");
    }
  }

  private void applySelfEffect(CardEffect effect, CharacterEffectGateway playerStats) {
    switch (effect.type()) {
      case BLOCK -> playerStats.gainBlock(effect.value());
      case HEAL -> playerStats.heal(effect.value());
      case STRENGTH -> playerStats.gainStrength(effect.value());
      case DAMAGE, POISON, VULNERABLE ->
          throw new IllegalArgumentException(
              "Unsupported self-targeting effect type: " + effect.type());
    }
  }

  private ResolvedCardEffect resolveEnemyEffect(
      String cardId, CardEffect effect, TargetType target, CharacterEffectGateway playerStats) {
    return switch (effect.type()) {
      case DAMAGE ->
          new ResolvedCardEffect(
              cardId,
              EffectType.DAMAGE,
              target,
              Math.max(0, effect.value() + playerStats.getStrengthModifier()),
              0);
      case POISON, VULNERABLE ->
          new ResolvedCardEffect(cardId, effect.type(), target, effect.value(), effect.duration());
      case BLOCK, HEAL, STRENGTH ->
          throw new IllegalArgumentException(
              "Unsupported enemy-targeting effect type: " + effect.type());
    };
  }
}
