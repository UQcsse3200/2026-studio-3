package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.PlayerRunState;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardIntegrationTest {

  @Test
  void claimedGoldIsImmediatelyStoredInRunState() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);

    playerState.addGold(25);

    assertEquals(75, playerState.getGold());
  }

  @Test
  void luckyCoinBoostsAndIsConsumedOnlyWhenGoldIsClaimed() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    playerState.addOwnedItem(ItemType.LUCKY_COIN);

    RewardService service = new RewardService(new RewardGenerator(new Random(42)));

    List<RewardOption> options = service.generateRewardOptions();

    RewardOption goldOption = options.get(0);

    RewardGenerator baseGenerator = new RewardGenerator(new Random(42));

    int baseAmount = baseGenerator.generateGoldRewardOption().goldAmount;

    assertEquals(baseAmount, goldOption.goldAmount);

    int previewBonus = playerState.calculateLuckyCoinBonus(goldOption.goldAmount);
    playerState.claimGoldReward(goldOption.goldAmount, true);

    int subtotal = 50 + goldOption.goldAmount;
    assertEquals(subtotal + previewBonus, playerState.getGold());
    assertEquals(0, playerState.getOwnedItemCount(ItemType.LUCKY_COIN));
    assertEquals(0f, playerState.getGoldBonusMultiplier(), 0.001f);
  }

  @Test
  void luckyCoinIsRetainedWhenAnItemRewardIsChosen() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    playerState.addOwnedItem(ItemType.LUCKY_COIN);

    playerState.addOwnedItem(ItemType.ENERGY_CRYSTAL);

    assertEquals(1, playerState.getOwnedItemCount(ItemType.LUCKY_COIN));
    assertEquals(0.1f, playerState.getGoldBonusMultiplier(), 0.001f);
  }

  @Test
  void energyCrystalAddsOneMaximumEnergy() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    playerState.addOwnedItem(ItemType.ENERGY_CRYSTAL);

    Entity player = createPlayer();
    playerState.applyTo(player);

    assertEquals(4, player.getComponent(EnergyComponent.class).getMaxEnergy());
  }

  @Test
  void merchantsFavorAddsTenPercentDiscount() {
    PlayerRunState playerState = new PlayerRunState(100, 100, 50);
    playerState.addOwnedItem(ItemType.MERCHANTS_FAVOR);

    Entity player = createPlayer();
    playerState.applyTo(player);

    assertEquals(0.10f, player.getComponent(InventoryComponent.class).getShopDiscount(), 0.001f);
  }

  private Entity createPlayer() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, 5, 100))
        .addComponent(new InventoryComponent(0))
        .addComponent(new EnergyComponent(3));
  }
}
