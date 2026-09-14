package com.csse3200.game.bestiary;

/** The amount of information the player has unlocked for a Bestiary entry. */
public enum BestiaryUnlockState {
  /** The enemy has not been encountered. */
  LOCKED,
  /** The enemy has been encountered, revealing its identity and artwork. */
  ENCOUNTERED,
  /** The enemy has been defeated, revealing its complete combat information. */
  DEFEATED;

  /**
   * Checks whether this state includes all information available at another state.
   *
   * @param required minimum state to compare against
   * @return true when this state is at least as advanced as {@code required}
   */
  public boolean isAtLeast(BestiaryUnlockState required) {
    return required != null && ordinal() >= required.ordinal();
  }
}
