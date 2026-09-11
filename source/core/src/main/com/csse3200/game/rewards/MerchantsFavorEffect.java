package com.csse3200.game.rewards;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

public class MerchantsFavorEffect implements ItemEffect {
    private static final float DISCOUNT_INCREMENT = 0.05f;
    private static final float DISCOUNT_CAP = 0.5f;

    @Override
    public void apply(Entity player) {
        InventoryComponent inventory = player.getComponent(InventoryComponent.class);
        inventory.addShopDiscount(DISCOUNT_INCREMENT);
    }
}