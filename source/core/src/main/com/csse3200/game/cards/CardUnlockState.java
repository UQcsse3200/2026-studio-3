package com.csse3200.game.cards;

/** The amount of information the player has unlocked for a card-library entry. */
public enum CardUnlockState {
  /** The card has not been seen. */
  LOCKED,
  /** The card has been seen, revealing its complete definition. */
  SEEN;

  /**
   * Checks whether this state includes all information available at another state.
   *
   * @param required minimum state to compare against
   * @return true when this state is at least as advanced as {@code required}
   */
  public boolean isAtLeast(CardUnlockState required) {
    return required != null && ordinal() >= required.ordinal();
  }
}
