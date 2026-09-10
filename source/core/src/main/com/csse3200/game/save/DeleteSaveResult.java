package com.csse3200.game.save;

/** Result returned after attempting to delete a save slot. */
public record DeleteSaveResult(boolean success, SaveError error, String message) {
  public static DeleteSaveResult succeeded() {
    return new DeleteSaveResult(true, SaveError.NONE, "");
  }

  public static DeleteSaveResult failure(SaveError error, String message) {
    return new DeleteSaveResult(false, error, message == null ? "" : message);
  }
}
