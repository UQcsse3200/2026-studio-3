package com.csse3200.game.rewards;

public class RewardOption {
  public RewardType type;
  public int goldAmount;
  public ItemType itemId;

  public RewardOption(RewardType type) {
    this.type = type;
  }
}
