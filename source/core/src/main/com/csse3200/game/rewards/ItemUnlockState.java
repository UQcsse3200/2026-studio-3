package com.csse3200.game.rewards;

/** The amount of information the player has unlocked for an Item Library entry. */
public enum ItemUnlockState {
  /** The item has not been legitimately obtained. */
  LOCKED,
  /** The item has been legitimately obtained and its Library entry is fully visible. */
  DISCOVERED;

  /**
   * Checks whether this state includes all information available at another state.
   *
   * @param required minimum state to compare against
   * @return true when this state is at least as advanced as {@code required}
   */
  public boolean isAtLeast(ItemUnlockState required) {
    return required != null && ordinal() >= required.ordinal();
  }
}
