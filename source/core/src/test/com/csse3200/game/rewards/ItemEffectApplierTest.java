package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
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

  @Test
  void shouldDispatchToNewCombatItemEffects() {
    Entity player = new Entity();
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    player.addComponent(stats);

    ItemEffectApplier.applyItemEffect(ItemType.IRON_AEGIS, player);
    ItemEffectApplier.applyItemEffect(ItemType.WARRIORS_CREST, player);

    assertEquals(5, stats.getArmour());
    assertEquals(6, stats.getBaseAttack());
  }
}
