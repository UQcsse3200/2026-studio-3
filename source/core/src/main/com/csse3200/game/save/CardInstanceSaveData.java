package com.csse3200.game.save;

/**
 * Serializable snapshot of one owned card copy — instance identity plus upgrade state.
 *
 * <p>Mirrors {@link com.csse3200.game.cards.runtime.CardInstance}'s three fields as a plain,
 * independently-serialisable DTO, following the same pattern as {@link MapNodeSaveData}: never
 * serialise the live object directly.
 */
public class CardInstanceSaveData {
  public String instanceId;
  public String cardId;
  public int upgradeLevel;

  /** Required for JSON deserialisation. */
  public CardInstanceSaveData() {}

  public CardInstanceSaveData(String instanceId, String cardId, int upgradeLevel) {
    this.instanceId = instanceId;
    this.cardId = cardId;
    this.upgradeLevel = upgradeLevel;
  }
}
