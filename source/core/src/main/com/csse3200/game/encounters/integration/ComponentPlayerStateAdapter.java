package com.csse3200.game.encounters.integration;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;

/** Adapts the current Team 7 player components to the non-battle encounter contract. */
public final class ComponentPlayerStateAdapter implements PlayerStateGateway {
  private final CombatStatsComponent combatStats;
  private final InventoryComponent inventory;

  /**
   * Creates an adapter for one player.
   *
   * @param combatStats component containing player health
   * @param inventory component containing player currency
   */
  public ComponentPlayerStateAdapter(
      CombatStatsComponent combatStats, InventoryComponent inventory) {
    this.combatStats = Objects.requireNonNull(combatStats, "combatStats cannot be null");
    this.inventory = Objects.requireNonNull(inventory, "inventory cannot be null");
  }

  @Override
  public int getHealth() {
    return combatStats.getHealth();
  }

  @Override
  public void setHealth(int health) {
    combatStats.setHealth(health);
  }

  @Override
  public int getCurrency() {
    return inventory.getGold();
  }

  @Override
  public void setCurrency(int currency) {
    inventory.setGold(currency);
  }
}
