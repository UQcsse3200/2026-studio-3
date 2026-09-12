package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LuckyCoinEffectTest {

    @Test
    void shouldIncreaseGoldBonusMultiplier() {
        Entity player = new Entity();
        InventoryComponent inventory = new InventoryComponent(0);
        player.addComponent(inventory);

        new LuckyCoinEffect().apply(player);

        assertEquals(0.1f, inventory.getGoldBonusMultiplier(), 0.001f);
    }

    @Test
    void multipleLuckyCoinsShouldStack() {
        // 验证是 += 而不是覆盖赋值，拿两次应该叠加
        Entity player = new Entity();
        InventoryComponent inventory = new InventoryComponent(0);
        player.addComponent(inventory);

        new LuckyCoinEffect().apply(player);
        new LuckyCoinEffect().apply(player);

        assertEquals(0.2f, inventory.getGoldBonusMultiplier(), 0.001f);
    }

    @Test
    void bonusShouldPersistAndAffectFutureGoldRewards() {
        // 验证加成是永久的，拿到后再领取金币奖励，加成依然生效
        Entity player = new Entity();
        InventoryComponent inventory = new InventoryComponent(0);
        player.addComponent(inventory);

        new LuckyCoinEffect().apply(player);
        new GoldReward(30).apply(player);

        assertEquals(33, inventory.getGold()); // 30 * 1.1 = 33
    }
}