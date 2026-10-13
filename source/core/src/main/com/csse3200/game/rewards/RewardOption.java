package com.csse3200.game.rewards;

/** One immutable, internally consistent top-level post-battle reward choice. */
public final class RewardOption {
  public final RewardType type;
  public final int goldAmount;
  public final ItemType itemId;
  public final CardRewardSelection cardSelection;

  private RewardOption(
      RewardType type, int goldAmount, ItemType itemId, CardRewardSelection cardSelection) {
    this.type = type;
    this.goldAmount = goldAmount;
    this.itemId = itemId;
    this.cardSelection = cardSelection;
  }

  public static RewardOption gold(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("gold amount must not be negative");
    }
    return new RewardOption(RewardType.GOLD, amount, null, null);
  }

  public static RewardOption item(ItemType itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("itemId must not be null");
    }
    return new RewardOption(RewardType.ITEM, 0, itemId, null);
  }

  public static RewardOption cards(CardRewardSelection selection) {
    if (selection == null) {
      throw new IllegalArgumentException("card selection must not be null");
    }
    return new RewardOption(RewardType.CARD, 0, null, selection);
  }
}
