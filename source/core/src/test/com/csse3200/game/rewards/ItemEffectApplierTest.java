package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemEffectApplierTest {

  @Test
  void shouldDispatchToEnergyCrystalEffect() {
    Entity player = new Entity();
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);

    ItemEffectApplier.applyItemEffect(ItemType.ENERGY_CRYSTAL, player);

    assertEquals(4, energy.getMaxEnergy());
  }
}
