package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardServiceTest {

  @Test
  void shouldGenerateOneGoldAndOneItemOption() {
    RewardService service = new RewardService(new RewardGenerator(new Random(1)));
    List<RewardOption> options = service.generateRewardOptions();

    assertEquals(2, options.size());
    assertEquals(RewardType.GOLD, options.get(0).type);
    assertEquals(RewardType.ITEM, options.get(1).type);
  }

  @Test
  void shouldGenerateRequestedNumberOfOptions() {
    RewardService service = new RewardService(new RewardGenerator(new Random(1)));
    List<RewardOption> options = service.generateRewardOptions();
    assertEquals(2, options.size());
  }

  @Test
  void claimingGoldRewardShouldAddGoldToPlayer() {
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));

    RewardService service = new RewardService(new RewardGenerator(new Random(1)));
    RewardOption option = new RewardOption(RewardType.GOLD);
    option.goldAmount = 25;

    service.claimReward(player, option);

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void claimingCardUpgradeShouldThrowUnsupportedForNow() {
    Entity player = new Entity();
    RewardService service = new RewardService(new RewardGenerator(new Random(1)));
    RewardOption option = new RewardOption(RewardType.CARD_UPGRADE);
    option.cardId = "some-card-id";

    assertThrows(UnsupportedOperationException.class, () -> service.claimReward(player, option));
  }

  @Test
  void claimingOneOptionShouldNotAffectTheOther() {
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));

    RewardService service = new RewardService(new RewardGenerator(new Random(1)));
    RewardOption goldOption = new RewardOption(RewardType.GOLD);
    goldOption.goldAmount = 25;

    service.claimReward(player, goldOption);

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
  }
}
