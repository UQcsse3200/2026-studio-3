package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.rewards.RewardType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardDisplayTest {

  @Test
  void checkpointsAppliedRewardBeforeNavigationAndOnlyOnce() {
    RunState runState = new RunState();
    PlayerRunState playerState = runState.getOrCreatePlayerState();
    int originalGold = playerState.getGold();
    RewardOption reward = new RewardOption(RewardType.GOLD);
    reward.goldAmount = 25;

    AtomicBoolean checkpointComplete = new AtomicBoolean();
    AtomicInteger checkpointCalls = new AtomicInteger();
    RewardDisplay display =
        new RewardDisplay(
            DisplayingRecord.builder("").build(),
            mock(RewardService.class),
            runState,
            () -> {
              assertEquals(originalGold + 25, playerState.getGold());
              checkpointCalls.incrementAndGet();
              checkpointComplete.set(true);
            });
    Entity entity = new Entity().addComponent(display);
    entity
        .getEvents()
        .addListener(
            RewardDisplay.REWARD_CLAIMED_EVENT,
            () -> assertTrue(checkpointComplete.get(), "claim event preceded checkpoint"));
    entity
        .getEvents()
        .addListener(
            EndBattleDisplay.RETURN_TO_MENU_EVENT,
            () -> assertTrue(checkpointComplete.get(), "navigation preceded checkpoint"));

    display.claimOption(reward);
    display.claimOption(reward);

    assertEquals(originalGold + 25, playerState.getGold());
    assertEquals(1, checkpointCalls.get());
  }
}
