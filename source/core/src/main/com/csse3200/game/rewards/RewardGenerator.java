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

  /**
   * Generates exactly one gold reward option.
   *
   * @return a randomly generated gold reward option
   */
  public RewardOption generateGoldRewardOption() {
    GoldReward goldReward = generateGoldOption();
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = goldReward.getBaseAmount();
    return option;
  }

  /**
   * Generates exactly one item reward option, randomly chosen from the eligible item pool.
   *
   * @return a randomly generated item reward option
   */
  public RewardOption generateItemRewardOption() {
    ItemType[] items = ItemType.values();
    ItemType picked = items[random.nextInt(items.length)];
    RewardOption option = new RewardOption(RewardType.ITEM);
    option.itemId = picked;
    return option;
  }

  /**
   * Generates a single reward option, randomly choosing between a gold reward and an item reward.
   * Kept for compatibility with callers that just need one arbitrary option.
   *
   * @return a randomly generated reward option
   */
  public RewardOption generateRewardOption() {
    return random.nextBoolean() ? generateGoldRewardOption() : generateItemRewardOption();
  }
}
