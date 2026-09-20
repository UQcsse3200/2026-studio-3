package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;

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
    int totalValue = 0;
    if (card != null && card.effects != null) {
      for (EffectConfig effect : card.effects) {
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
