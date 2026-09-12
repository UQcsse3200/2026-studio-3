package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RunStatePendingRewardTest {

  @Test
  void shouldHaveNoPendingRewardInitially() {
    RunState runState = new RunState();
    assertNull(runState.getPendingReward());
  }

  @Test
  void shouldStoreAndRetrievePendingReward() {
    RunState runState = new RunState();
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = 50;

    runState.setPendingReward(option);

    assertEquals(option, runState.getPendingReward());
  }

  @Test
  void shouldClearPendingReward() {
    RunState runState = new RunState();
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = 50;
    runState.setPendingReward(option);

    runState.clearPendingReward();

    assertNull(runState.getPendingReward());
  }

  @Test
  void shouldOverwritePreviousPendingReward() {
    RunState runState = new RunState();
    RewardOption first = new RewardOption(RewardType.GOLD);
    first.goldAmount = 50;
    RewardOption second = new RewardOption(RewardType.ITEM);
    second.itemId = ItemType.ENERGY_CRYSTAL;

    runState.setPendingReward(first);
    runState.setPendingReward(second);

    assertEquals(second, runState.getPendingReward());
  }
}
