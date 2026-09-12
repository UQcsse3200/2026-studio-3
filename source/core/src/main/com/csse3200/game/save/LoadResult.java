package com.csse3200.game.save;

/** Result returned after attempting to read and parse a save slot. */
public record LoadResult(boolean success, SaveGameData data, SaveError error, String message) {
  public static LoadResult success(SaveGameData data) {
    return new LoadResult(true, data, SaveError.NONE, "");
  }

  public static LoadResult failure(SaveError error, String message) {
    return new LoadResult(false, null, error, message == null ? "" : message);
  }
}
