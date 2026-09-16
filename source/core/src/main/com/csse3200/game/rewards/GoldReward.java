package com.csse3200.game.rewards;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

public class GoldReward {
  private final int baseAmount;

  public GoldReward(int baseAmount) {
    this.baseAmount = baseAmount;
  }

  public int getBaseAmount() {
    return baseAmount;
  }

  public void apply(Entity player) {
    int finalAmount = calculateFinalGold(player, baseAmount);
    player.getComponent(InventoryComponent.class).addGold(finalAmount);
  }

  public static int calculateFinalGold(Entity player, int baseAmount) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    return Math.round(baseAmount * (1 + inventory.getGoldBonusMultiplier()));
  }
}
