package com.csse3200.game.rewards;

import com.csse3200.game.entities.Entity;

public class ItemReward {
  private final ItemType itemType;

  public ItemReward(ItemType itemType) {
    this.itemType = itemType;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public void apply(Entity player) {
    ItemEffectApplier.applyItemEffect(itemType, player);
  }
}
