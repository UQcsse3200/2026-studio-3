package com.csse3200.game.rewards;

/** Identifies how an item entered a run so discovery can follow the agreed game rules. */
public enum ItemAcquisitionSource {
  /** A player chose an item from a normal post-battle reward. */
  NORMAL_REWARD(true),
  /** A player accepted an item from a completed Chance Event outcome. */
  CHANCE_EVENT(true),
  /** A development-only command granted an item for testing or previewing UI. */
  DEBUG(false);

  private final boolean discoversItem;

  ItemAcquisitionSource(boolean discoversItem) {
    this.discoversItem = discoversItem;
  }

  /** Returns whether a successful acquisition from this source permanently reveals the item. */
  public boolean discoversItem() {
    return discoversItem;
  }
}
