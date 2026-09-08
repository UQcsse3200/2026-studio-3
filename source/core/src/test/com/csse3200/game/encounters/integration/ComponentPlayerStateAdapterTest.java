package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import org.junit.jupiter.api.Test;

class ComponentPlayerStateAdapterTest {
  @Test
  void shouldReadAndUpdateCurrentPlayerComponents() {
    CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
    InventoryComponent inventory = new InventoryComponent(50);
    ComponentPlayerStateAdapter player = new ComponentPlayerStateAdapter(combatStats, inventory);

    assertEquals(100, player.getHealth());
    assertEquals(50, player.getCurrency());

    player.setHealth(80);
    player.setCurrency(35);

    assertEquals(80, combatStats.getHealth());
    assertEquals(35, inventory.getGold());
  }
}
