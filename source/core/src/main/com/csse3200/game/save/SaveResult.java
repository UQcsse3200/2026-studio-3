package com.csse3200.game.save;

/** Result returned after attempting to write a save slot. */
public record SaveResult(
    boolean success, SaveSlotMetadata metadata, SaveError error, String message) {
  public static SaveResult success(SaveSlotMetadata metadata) {
    return new SaveResult(true, metadata, SaveError.NONE, "");
  }

  public static SaveResult failure(SaveError error, String message) {
    return new SaveResult(false, null, error, message == null ? "" : message);
  }
}
