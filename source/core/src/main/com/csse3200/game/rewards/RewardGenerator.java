package com.csse3200.game.rewards;

import java.util.Random;

public class RewardGenerator {
  private static final int MIN_GOLD = 20;
  private static final int MAX_GOLD = 30;
  private final Random random;

  public RewardGenerator() {
    this(new Random());
  }

  public RewardGenerator(Random random) {
    this.random = random;
  }

  public GoldReward generateGoldOption() {
    int base = MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1);
    return new GoldReward(base);
  }

  /** Generates a GOLD-type RewardOption with a randomly rolled amount. */
  public RewardOption generateGoldRewardOption() {
    GoldReward goldReward = generateGoldOption();
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = goldReward.getBaseAmount();
    return option;
  }

  /** Generates an ITEM-type RewardOption with a randomly picked item type. */
  public RewardOption generateItemRewardOption() {
    ItemType[] items = ItemType.values();
    ItemType picked = items[random.nextInt(items.length)];
    RewardOption option = new RewardOption(RewardType.ITEM);
    option.itemId = picked;
    return option;
  }

  public RewardOption generateRewardOption() {
    return generateGoldRewardOption();
  }
}
