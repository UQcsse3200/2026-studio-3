package com.csse3200.game.save;

/** Result returned after attempting to apply loaded save data to the live game state. */
public record RestoreResult(
    boolean success, RestoreError error, String message, String resumeScreen) {
  public static RestoreResult success(String resumeScreen) {
    return new RestoreResult(true, RestoreError.NONE, "", resumeScreen == null ? "" : resumeScreen);
  }

  public static RestoreResult failure(RestoreError error, String message) {
    return new RestoreResult(false, error, message == null ? "" : message, "");
  }
}
