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

  public List<RewardOption> generateRewardOptions(int numOptions) {
    List<RewardOption> options = new ArrayList<>();
    for (int i = 0; i < numOptions; i++) {
      options.add(generator.generateRewardOption());
    }
    return options;
  }

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
