package com.csse3200.game.save;

import java.util.List;

/** Result returned when enumerating save slots. */
public record SaveSlotListResult(
    boolean success, List<SaveSlotMetadata> slots, SaveError error, String message) {
  public SaveSlotListResult {
    slots = slots == null ? List.of() : List.copyOf(slots);
    message = message == null ? "" : message;
  }

  public static SaveSlotListResult success(List<SaveSlotMetadata> slots) {
    return new SaveSlotListResult(true, slots, SaveError.NONE, "");
  }

  public static SaveSlotListResult failure(SaveError error, String message) {
    return new SaveSlotListResult(false, List.of(), error, message);
  }
}
