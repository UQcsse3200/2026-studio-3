package com.csse3200.game.save;

/** Stable error codes returned when validated save data cannot be applied to live game state. */
public enum RestoreError {
  NONE,
  NO_SAVE_DATA,
  MISSING_PLAYER_DATA,
  MISSING_DECK_DATA,
  MISSING_MAP_DATA,
  INVALID_PLAYER_STATE,
  INVALID_DECK_STATE,
  INVALID_MAP_STATE,
  APPLY_FAILED
}
