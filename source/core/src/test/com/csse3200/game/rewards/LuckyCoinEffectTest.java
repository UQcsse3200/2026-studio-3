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
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(0);
    player.addComponent(inventory);

    new LuckyCoinEffect().apply(player);
    new LuckyCoinEffect().apply(player);

    assertEquals(0.2f, inventory.getGoldBonusMultiplier(), 0.001f);
  }

  @Test
  void bonusShouldPersistAndAffectFutureGoldRewards() {
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(0);
    player.addComponent(inventory);

    new LuckyCoinEffect().apply(player);
    new GoldReward(30).apply(player);

    assertEquals(33, inventory.getGold()); // 30 * 1.1 = 33
  }
}
