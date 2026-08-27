package com.csse3200.game.cards.effects;

import java.util.ArrayList;
import java.util.List;

/** Resolves a card's effects without taking ownership of player or enemy state. */
public class CardEffectResolver {
  private final EffectExecutor effectExecutor;

  public CardEffectResolver() {
    this(new EffectExecutor());
  }

  public CardEffectResolver(EffectExecutor effectExecutor) {
    if (effectExecutor == null) {
      throw new IllegalArgumentException("Effect executor cannot be null");
    }
    this.effectExecutor = effectExecutor;
  }

  /**
   * Resolves a prepared card effect request.
   *
   * <p>The returned list contains only effects that need another system to apply them, currently
   * enemy-targeting effects. Self effects are sent directly through {@code playerStats}.
   */
  public List<ResolvedCardEffect> resolve(
      CardEffectRequest request, CharacterEffectGateway playerStats) {
    validate(request, playerStats);

    List<ResolvedCardEffect> resolvedEffects = new ArrayList<>();
    for (CardEffect effect : request.effects()) {
      resolvedEffects.addAll(
          effectExecutor.execute(request.cardId(), effect, request.target(), playerStats));
    }
    return List.copyOf(resolvedEffects);
  }

  public List<ResolvedCardEffect> resolve(
      String cardId,
      TargetType target,
      List<CardEffect> effects,
      CharacterEffectGateway playerStats) {
    return resolve(new CardEffectRequest(cardId, target, effects), playerStats);
  }

  private void validate(CardEffectRequest request, CharacterEffectGateway playerStats) {
    if (request == null) {
      throw new IllegalArgumentException("Card effect request cannot be null");
    }
    if (playerStats == null) {
      throw new IllegalArgumentException("Player stats gateway cannot be null");
    }
  }
}
