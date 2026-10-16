package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
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

  @Test
  void shouldDispatchToNewCombatItemEffects() {
    Entity player = new Entity();
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    player.addComponent(stats);

    ItemEffectApplier.applyItemEffect(ItemType.IRON_AEGIS, player);
    ItemEffectApplier.applyItemEffect(ItemType.WARRIORS_CREST, player);

    assertEquals(5, stats.getArmour());
    assertEquals(1, stats.getStatusEffect("STRENGTH").getValue());
  }

  @Test
  void shouldDispatchToGoldAndShopItemEffects() {
    InventoryComponent inventory = new InventoryComponent(0);
    Entity player = new Entity().addComponent(inventory);

    ItemEffectApplier.applyItemEffect(ItemType.LUCKY_COIN, player);
    ItemEffectApplier.applyItemEffect(ItemType.MERCHANTS_FAVOR, player);

    assertEquals(0.1f, inventory.getGoldBonusMultiplier(), 0.001f);
    assertEquals(0.1f, inventory.getShopDiscount(), 0.001f);
  }

  @Test
  void shouldRestoreTwentyHealthWithoutExceedingMaxHealth() {
    Entity player = new Entity();
    CombatStatsComponent stats = new CombatStatsComponent(85, 5, 100);
    player.addComponent(stats);

    ItemEffectApplier.applyItemEffect(ItemType.AMBROSIA_FLASK, player);

    assertEquals(100, stats.getHealth());
  }
}
