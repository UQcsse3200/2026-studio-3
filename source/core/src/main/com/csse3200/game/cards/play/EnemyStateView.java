package com.csse3200.game.cards.play;

import com.csse3200.game.cards.EffectType;

/**
 * Read-only Team 5 boundary for target-enemy values that can affect card resolution.
 *
 * <p>The target ID is deliberately opaque. Team 3 and Team 1 can agree on the concrete identifier
 * without making Team 5 depend on an enemy entity class.
 */
public interface EnemyStateView {
  /**
   * @return whether the selected target currently exists and can receive a card effect
   */
  boolean isTargetAvailable(String targetId);

  /**
   * Returns the current value/stacks for a target status, or zero when it is not active.
   *
   * @param targetId opaque ID supplied in {@link CardPlayTarget}
   * @param type status type, such as VULNERABLE or FEEBLE
   * @return current status value/stacks
   */
  int statusValue(String targetId, EffectType type);
}
