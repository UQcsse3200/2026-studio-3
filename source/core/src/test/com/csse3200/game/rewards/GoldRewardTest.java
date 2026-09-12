package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GoldRewardTest {

  @Test
  void shouldApplyBaseGoldWithoutBonus() {
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));

    GoldReward reward = new GoldReward(25);
    reward.apply(player);

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void shouldApplyLuckyCoinBonusCorrectly() {
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addGoldBonusMultiplier(0.1f);
    player.addComponent(inventory);

    GoldReward reward = new GoldReward(20);
    reward.apply(player);

    assertEquals(22, player.getComponent(InventoryComponent.class).getGold()); // 20 * 1.1 = 22
  }

  @Test
  void shouldReturnCorrectBaseAmount() {
    GoldReward reward = new GoldReward(30);
    assertEquals(30, reward.getBaseAmount());
  }

  @Test
  void calculateFinalGoldShouldMatchApplyResult() {
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addGoldBonusMultiplier(0.2f);
    player.addComponent(inventory);

    int finalGold = GoldReward.calculateFinalGold(player, 50);

    assertEquals(60, finalGold); // 50 * 1.2 = 60
  }
}
