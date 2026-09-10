package com.csse3200.game.rewards;

import com.csse3200.game.entities.Entity;

public class ItemEffectApplier {
  public static void applyItemEffect(ItemType itemId, Entity player) {
    if (itemId == ItemType.LUCKY_COIN) {
      new LuckyCoinEffect().apply(player);
    }
  }
}
