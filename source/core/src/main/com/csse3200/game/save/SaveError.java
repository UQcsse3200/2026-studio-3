package com.csse3200.game.save;

/** Stable error codes returned by save/load operations. */
public enum SaveError {
  NONE,
  INVALID_SLOT,
  NO_SAVE_DATA,
  CAPTURE_FAILED,
  SLOT_NOT_FOUND,
  MALFORMED_SAVE,
  UNSUPPORTED_VERSION,
  IO_ERROR
}
