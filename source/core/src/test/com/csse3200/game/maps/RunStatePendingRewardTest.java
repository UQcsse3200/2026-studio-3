package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardOption;
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
    RewardOption option = RewardOption.gold(50);

    runState.setPendingReward(option);

    assertEquals(option, runState.getPendingReward());
  }

  @Test
  void shouldClearPendingReward() {
    RunState runState = new RunState();
    RewardOption option = RewardOption.gold(50);
    runState.setPendingReward(option);

    runState.clearPendingReward();

    assertNull(runState.getPendingReward());
  }

  @Test
  void shouldOverwritePreviousPendingReward() {
    RunState runState = new RunState();
    RewardOption first = RewardOption.gold(50);
    RewardOption second = RewardOption.item(ItemType.ENERGY_CRYSTAL);

    runState.setPendingReward(first);
    runState.setPendingReward(second);

    assertEquals(second, runState.getPendingReward());
  }
}
