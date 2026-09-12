package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemEffectTest {

  @Test
  void energyCrystalShouldIncreaseMaxEnergyByOne() {
    Entity player = new Entity();
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);

    new EnergyCrystalEffect().apply(player);

    assertEquals(4, energy.getMaxEnergy());
  }

  @Test
  void energyCrystalShouldStackAcrossMultipleClaims() {
    Entity player = new Entity();
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);

    new EnergyCrystalEffect().apply(player);
    new EnergyCrystalEffect().apply(player);

    assertEquals(5, energy.getMaxEnergy());
  }

  @Test
  void merchantsFavorShouldIncreaseShopDiscount() {
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(100);
    player.addComponent(inventory);

    new MerchantsFavorEffect().apply(player);

    assertEquals(0.05f, inventory.getShopDiscount(), 0.001f);
  }

  @Test
  void merchantsFavorShouldRespectDiscountCap() {
    Entity player = new Entity();
    InventoryComponent inventory = new InventoryComponent(100);
    player.addComponent(inventory);

    for (int i = 0; i < 11; i++) {
      new MerchantsFavorEffect().apply(player);
    }

    assertEquals(0.5f, inventory.getShopDiscount(), 0.001f);
  }
}
