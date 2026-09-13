package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import java.util.List;

/** Immutable result of resolving one played card. */
public record CardEffectResolution(String cardId, List<ResolvedCardEffect> effects) {
  public CardEffectResolution {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }
    if (effects == null) {
      throw new IllegalArgumentException("Resolved effects cannot be null");
    }
    try {
      effects = List.copyOf(effects);
    } catch (NullPointerException exception) {
      throw new IllegalArgumentException("Resolved effects cannot contain null", exception);
    }
  }

  /**
   * @return effects that another system can apply to enemies
   */
  public List<ResolvedCardEffect> enemyEffects() {
    return effects.stream().filter(effect -> effect.target() != TargetType.SELF).toList();
  }

  /**
   * @return effects that another system can apply to the player
   */
  public List<ResolvedCardEffect> playerEffects() {
    return effects.stream().filter(effect -> effect.target() == TargetType.SELF).toList();
  }

  /**
   * @param type effect type to select
   * @return resolved effects with the requested type
   */
  public List<ResolvedCardEffect> effectsOfType(EffectType type) {
    if (type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    return effects.stream().filter(effect -> effect.type() == type).toList();
  }
}
