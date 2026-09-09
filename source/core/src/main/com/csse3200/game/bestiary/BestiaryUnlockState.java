package com.csse3200.game.bestiary;

/** Progress states used by the bestiary UI. */
public enum BestiaryUnlockState {
  /** The enemy has not been encountered and its information must remain hidden. */
  LOCKED,
  /** The enemy has been encountered at least once. */
  ENCOUNTERED,
  /** The enemy has been defeated at least once. */
  DEFEATED
}
