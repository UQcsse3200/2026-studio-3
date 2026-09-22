package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.Arrays;

/**
 * Derives how many player rounds a played card spends in the discard pile before it is
 * automatically retrieved back into the hand.
 */
public final class CardCooldown {
  private static final int LOW_VALUE_MAX = 4;
  private static final int MID_VALUE_MAX = 8;

  private static final int MIN_ROUNDS = 1;
  private static final int MAX_ROUNDS = 3;

  private CardCooldown() {}

  /**
   * @param card card to evaluate
   * @return 1-3 player rounds, derived from the sum of the values of every effect on the card
   */
  public static int roundsFor(CardConfig card) {
    return roundsForEffects(
        card == null || card.effects == null ? java.util.List.of() : Arrays.asList(card.effects));
  }

  /** Derives cooldown from the same resolved values used by gameplay. */
  public static int roundsFor(ResolvedCard card) {
    return roundsForEffects(card == null ? java.util.List.of() : card.effects());
  }

  private static int roundsForEffects(Iterable<EffectConfig> effects) {
    int totalValue = 0;
    for (EffectConfig effect : effects) {
      if (effect != null) {
        totalValue += effect.value;
      }
    }

    if (totalValue <= LOW_VALUE_MAX) {
      return MIN_ROUNDS;
    }
    if (totalValue <= MID_VALUE_MAX) {
      return 2;
    }
    return MAX_ROUNDS;
  }
}
