package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardIntegrationTest {

  @Test
  void pendingRewardFlowsCorrectlyFromRunStateToPlayer() {
    // Simulates the player selecting an option on RewardDisplay
    RunState runState = new RunState();
    RewardOption selectedOption = new RewardOption(RewardType.GOLD);
    selectedOption.goldAmount = 25;
    runState.setPendingReward(selectedOption);

    // Simulates BattleScreen reading and applying the pending reward
    // (mirrors the real logic in BattleScreen.java lines 129-137)
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));
    RewardService rewardService = new RewardService();

    RewardOption pendingReward = runState.getPendingReward();
    if (pendingReward != null) {
      rewardService.claimReward(player, pendingReward);
      runState.clearPendingReward();
    }

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
    assertNull(runState.getPendingReward());
  }

  @Test
  void noRewardMeansNoChangeToPlayerGold() {
    RunState runState = new RunState();
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(50));
    RewardService rewardService = new RewardService();

    RewardOption pendingReward = runState.getPendingReward();
    if (pendingReward != null) {
      rewardService.claimReward(player, pendingReward);
      runState.clearPendingReward();
    }

    assertEquals(50, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void itemRewardShouldApplyLuckyCoinBonusCorrectly() {
    RunState runState = new RunState();
    RewardOption luckyCoin = new RewardOption(RewardType.ITEM);
    luckyCoin.itemId = ItemType.LUCKY_COIN;
    runState.setPendingReward(luckyCoin);

    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));
    RewardService rewardService = new RewardService();

    RewardOption pending = runState.getPendingReward();
    rewardService.claimReward(player, pending);
    runState.clearPendingReward();

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    assertEquals(0.1f, inventory.getGoldBonusMultiplier(), 0.001f);

    RewardOption goldReward = new RewardOption(RewardType.GOLD);
    goldReward.goldAmount = 20;
    rewardService.claimReward(player, goldReward);

    assertEquals(22, inventory.getGold());
  }

  @Test
  void demonstratesTheKnownPersistenceIssue() {
    // Intentionally simulates a new Entity being created each battle,
    // matching real behaviour in PlayerFactory.createPlayer().
    // Documents the known limitation: bonuses do not survive across
    // a fresh Player entity.
    Entity player1 = new Entity();
    player1.addComponent(new InventoryComponent(0));
    RewardService rewardService = new RewardService();

    RewardOption luckyCoin = new RewardOption(RewardType.ITEM);
    luckyCoin.itemId = ItemType.LUCKY_COIN;
    rewardService.claimReward(player1, luckyCoin);

    assertEquals(
        0.1f, player1.getComponent(InventoryComponent.class).getGoldBonusMultiplier(), 0.001f);

    Entity player2 = new Entity();
    player2.addComponent(new InventoryComponent(0));

    assertEquals(
        0f, player2.getComponent(InventoryComponent.class).getGoldBonusMultiplier(), 0.001f);
  }
}
