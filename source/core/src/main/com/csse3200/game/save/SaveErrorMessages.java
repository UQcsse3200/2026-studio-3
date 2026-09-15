package com.csse3200.game.save;

/**
 * Translates {@link SaveError} codes and raw messages into readable text for the UI.
 *
 * <p>Kept separate from any UI framework so it's testable without a running game, and reusable
 * across the save panel, pause menu, and anywhere else a save/load result needs to be shown.
 */
public class SaveErrorMessages {

  private SaveErrorMessages() {}

  /** Returns a user-facing message for a save attempt result. */
  public static String forSave(SaveResult result) {
    if (result.success()) {
      return "Game saved.";
    }
    return forError(result.error(), result.message());
  }

  /** Returns a user-facing message for a load attempt result. */
  public static String forLoad(LoadResult result) {
    if (result.success()) {
      return "Game loaded.";
    }
    return forError(result.error(), result.message());
  }

  /** Returns a user-facing message for a delete attempt result. */
  public static String forDelete(DeleteSaveResult result) {
    if (result.success()) {
      return "Save deleted.";
    }
    return forError(result.error(), result.message());
  }

  private static String forError(SaveError error, String rawMessage) {
    String base =
        switch (error) {
          case INVALID_SLOT -> "That save slot isn't valid.";
          case NO_SAVE_DATA -> "There's nothing to save right now.";
          case CAPTURE_FAILED -> "Something went wrong capturing your run. Nothing was saved.";
          case SLOT_NOT_FOUND -> "No save was found in that slot.";
          case MALFORMED_SAVE -> "That save file is corrupted and can't be loaded.";
          case UNSUPPORTED_VERSION -> "That save was made with a different game version.";
          case IO_ERROR -> "Couldn't read or write the save file. Check your storage.";
          case NONE -> "";
        };
    if (rawMessage == null || rawMessage.isBlank()) {
      return base;
    }
    return base + " (" + rawMessage + ")";
  }
}
