package com.csse3200.game.save;

/** Serializable unlock state for one card-library entry. */
public class CardProgressSaveData {
  public String cardId = "";
  public String unlockState = "";

  /** Required for JSON deserialisation. */
  public CardProgressSaveData() {}

  public CardProgressSaveData(String cardId, String unlockState) {
    this.cardId = cardId == null ? "" : cardId;
    this.unlockState = unlockState == null ? "" : unlockState;
  }
}
