package com.csse3200.game.save;

/** Lightweight information shown when listing save slots. */
public class SaveSlotMetadata {
  public int slotId;
  public long savedAtEpochMillis;
  public String runLabel = "";
  public long playtimeSeconds;
  public String resumeScreen = "";
  public boolean loadable = true;
  public String statusMessage = "";

  /** Required for JSON deserialisation. */
  public SaveSlotMetadata() {}

  /** Creates metadata for a loadable save slot. */
  public SaveSlotMetadata(
      int slotId,
      long savedAtEpochMillis,
      String runLabel,
      long playtimeSeconds,
      String resumeScreen) {
    this.slotId = slotId;
    this.savedAtEpochMillis = savedAtEpochMillis;
    this.runLabel = safe(runLabel);
    this.playtimeSeconds = Math.max(0, playtimeSeconds);
    this.resumeScreen = safe(resumeScreen);
  }

  /** Creates placeholder metadata for a slot whose file cannot be loaded. */
  public static SaveSlotMetadata unreadable(int slotId, String message) {
    SaveSlotMetadata metadata = new SaveSlotMetadata();
    metadata.slotId = slotId;
    metadata.loadable = false;
    metadata.statusMessage = safe(message);
    return metadata;
  }

  /** Creates an independent copy suitable for returning to UI callers. */
  public SaveSlotMetadata copy() {
    SaveSlotMetadata copy =
        new SaveSlotMetadata(slotId, savedAtEpochMillis, runLabel, playtimeSeconds, resumeScreen);
    copy.loadable = loadable;
    copy.statusMessage = safe(statusMessage);
    return copy;
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
