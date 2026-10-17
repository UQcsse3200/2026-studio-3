package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemRewardTest {
  @Test
  void exposesItsTypeAndAppliesTheMatchingEffect() {
    ItemReward reward = new ItemReward(ItemType.ENERGY_CRYSTAL);
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(energy);

    assertEquals(ItemType.ENERGY_CRYSTAL, reward.getItemType());
    reward.apply(player);
    assertEquals(4, energy.getMaxEnergy());
  }
}
