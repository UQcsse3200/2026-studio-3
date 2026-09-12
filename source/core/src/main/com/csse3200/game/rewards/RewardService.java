package com.csse3200.game.rewards;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;

public class RewardService {
  private final RewardGenerator generator;

  public RewardService() {
    this(new RewardGenerator());
  }

  public RewardService(RewardGenerator generator) {
    this.generator = generator;
  }

  /**
   * Generates a fixed set of reward options: one GOLD option and one ITEM option.
   *
   * @return a list containing exactly one GOLD RewardOption and one ITEM RewardOption
   */
  public List<RewardOption> generateRewardOptions() {
    List<RewardOption> options = new ArrayList<>();
    options.add(generator.generateGoldRewardOption());
    options.add(generator.generateItemRewardOption());
    return options;
  }

  /**
   * Applies the selected reward option to the player's actual state.
   *
   * @param player the player entity to apply the reward to
   * @param selected the reward option the player chose/claimed
   */
  public void claimReward(Entity player, RewardOption selected) {
    switch (selected.type) {
      case GOLD -> {
        int finalAmount = GoldReward.calculateFinalGold(player, selected.goldAmount);
        player.getComponent(InventoryComponent.class).addGold(finalAmount);
      }
      case CARD_UPGRADE -> {
        throw new UnsupportedOperationException("Card upgrade not yet implemented");
      }
      case ITEM -> ItemEffectApplier.applyItemEffect(selected.itemId, player);
    }
  }
}
