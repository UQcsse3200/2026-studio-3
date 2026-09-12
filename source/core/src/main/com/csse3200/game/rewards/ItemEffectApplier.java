package com.csse3200.game.rewards;

import com.csse3200.game.entities.Entity;

public class ItemEffectApplier {
  public static void applyItemEffect(ItemType itemId, Entity player) {
    switch (itemId) {
      case LUCKY_COIN -> new LuckyCoinEffect().apply(player);
      case ENERGY_CRYSTAL -> new EnergyCrystalEffect().apply(player); // 新增
      case MERCHANTS_FAVOR -> new MerchantsFavorEffect().apply(player); // 新增
    }
  }
}
