package com.csse3200.game.cards.play;

import com.csse3200.game.cards.EffectType;

/**
 * Read-only Team 5 boundary for player values that can affect card resolution.
 *
 * <p>The concrete adapter to Team 7 remains outside the card resolver so Team 7 stays the source of
 * truth for player combat state.
 */
public interface PlayerStateView {
  /**
   * @return the player's current energy
   */
  int currentEnergy();

  /**
   * Returns the current value/stacks for a card-related status, or zero when it is not active.
   *
   * @param type status type, such as STRENGTH or FEEBLE
   * @return current status value/stacks
   */
  int statusValue(EffectType type);
}
