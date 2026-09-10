package com.csse3200.game.rewards;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

public class LuckyCoinEffect implements ItemEffect {
  private static final float BONUS = 0.1f;

  @Override
  public void apply(Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addGoldBonusMultiplier(BONUS);
  }
}
