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

  // 目前只生成 GOLD 类型，2-3个选项供玩家选
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
        // TODO: 待 celia0419 确认 upgradeCard 接口后接入
        throw new UnsupportedOperationException("Card upgrade not yet implemented");
      }
      case ITEM -> ItemEffectApplier.applyItemEffect(selected.itemId, player);
    }
  }
}
