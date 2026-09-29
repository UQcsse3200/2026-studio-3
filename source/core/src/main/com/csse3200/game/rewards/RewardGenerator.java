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
    if (random == null) {
      throw new IllegalArgumentException("random must not be null");
    }
    this.random = random;
  }

  public GoldReward generateGoldOption() {
    int base = MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1);
    return new GoldReward(base);
  }

  /** Generates gold without an item multiplier. */
  public RewardOption generateGoldRewardOption() {
    return generateGoldRewardOption(0f);
  }

  /**
   * Generates the final gold amount, including the player's current Lucky Coin multiplier.
   *
   * @param goldBonusMultiplier accumulated gold bonus
   * @return final gold reward option
   */
  public RewardOption generateGoldRewardOption(float goldBonusMultiplier) {
    if (!Float.isFinite(goldBonusMultiplier) || goldBonusMultiplier < 0f) {
      throw new IllegalArgumentException("goldBonusMultiplier must be finite and non-negative");
    }

    int baseAmount = generateGoldOption().getBaseAmount();
    int finalAmount = Math.round(baseAmount * (1f + goldBonusMultiplier));

    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = finalAmount;
    return option;
  }

  public RewardOption generateItemRewardOption() {
    ItemType[] items = ItemType.values();
    ItemType picked = items[random.nextInt(items.length)];

    RewardOption option = new RewardOption(RewardType.ITEM);
    option.itemId = picked;
    return option;
  }

  public RewardOption generateRewardOption() {
    return random.nextBoolean() ? generateGoldRewardOption() : generateItemRewardOption();
  }
}
