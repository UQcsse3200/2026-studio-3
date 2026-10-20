package com.csse3200.game.save;

/** Serializable unlock state for one Item Library entry. */
public class ItemProgressSaveData {
  public String itemId = "";
  public String unlockState = "";

  /** Required for JSON deserialisation. */
  public ItemProgressSaveData() {}

  public ItemProgressSaveData(String itemId, String unlockState) {
    this.itemId = itemId == null ? "" : itemId;
    this.unlockState = unlockState == null ? "" : unlockState;
  }
}
