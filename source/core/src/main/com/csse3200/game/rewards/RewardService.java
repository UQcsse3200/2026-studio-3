package com.csse3200.game.rewards;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.List;

public class RewardService {
  private final RewardGenerator generator;

  public RewardService() {
    this(new RewardGenerator());
  }

  public RewardService(RewardGenerator generator) {
    if (generator == null) {
      throw new IllegalArgumentException("generator must not be null");
    }
    this.generator = generator;
  }

  /** Generates rewards for a player without a gold bonus. */
  public List<RewardOption> generateRewardOptions() {
    return generateRewardOptions(0f);
  }

  /**
   * Generates one final gold option and one item option.
   *
   * @param goldBonusMultiplier current Lucky Coin multiplier
   * @return one gold option and one item option
   */
  public List<RewardOption> generateRewardOptions(float goldBonusMultiplier) {
    return List.of(
        generator.generateGoldRewardOption(goldBonusMultiplier),
        generator.generateItemRewardOption());
  }

  /**
   * Applies a generated reward to a live player entity.
   *
   * <p>The gold amount has already been finalised by RewardGenerator and must not be multiplied
   * again here.
   */
  public void claimReward(Entity player, RewardOption selected) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    if (selected == null || selected.type == null) {
      throw new IllegalArgumentException("selected reward must not be null");
    }

    switch (selected.type) {
      case GOLD -> {
        InventoryComponent inventory = player.getComponent(InventoryComponent.class);

        if (inventory == null) {
          throw new IllegalArgumentException("player must have InventoryComponent");
        }

        inventory.addGold(selected.goldAmount);
      }

      case ITEM -> {
        if (selected.itemId == null) {
          throw new IllegalArgumentException("item reward must have an itemId");
        }
        ItemEffectApplier.applyItemEffect(selected.itemId, player);
      }
    }
  }
}
