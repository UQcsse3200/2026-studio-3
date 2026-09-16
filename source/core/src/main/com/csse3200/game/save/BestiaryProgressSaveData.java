package com.csse3200.game.save;

/** Serializable unlock state for one Bestiary entry. */
public class BestiaryProgressSaveData {
  public String enemyId = "";
  public String unlockState = "";

  /** Required for JSON deserialisation. */
  public BestiaryProgressSaveData() {}

  public BestiaryProgressSaveData(String enemyId, String unlockState) {
    this.enemyId = enemyId == null ? "" : enemyId;
    this.unlockState = unlockState == null ? "" : unlockState;
  }
}
