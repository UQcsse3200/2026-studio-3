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

  public RewardOption generateRewardOption() {
    GoldReward goldReward = generateGoldOption();
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = goldReward.getBaseAmount();
    return option;
  }
}
