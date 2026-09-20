package com.csse3200.game.maps;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

/**
 * Run-scoped player values that must survive screen disposal.
 *
 * <p>Gameplay screens create and dispose their own player entities. Keeping the durable values in
 * this small model prevents health and gold from silently resetting whenever the player leaves a
 * battle, and gives Save/Load a real source and destination that is independent of a rendered
 * entity.
 */
public class PlayerRunState {
  private int currentHealth;
  private int maxHealth;
  private int gold;

  public PlayerRunState(int currentHealth, int maxHealth, int gold) {
    restore(currentHealth, maxHealth, gold);
  }

  public int getCurrentHealth() {
    return currentHealth;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getGold() {
    return gold;
  }

  /** Applies the durable values to the player entity created for a gameplay screen. */
  public void applyTo(Entity player) {
    CombatStatsComponent stats = requireStats(player);
    InventoryComponent inventory = requireInventory(player);

    stats.setMaxHealth(maxHealth);
    stats.setHealth(currentHealth);
    inventory.setGold(gold);
  }

  /** Captures the latest values before a gameplay screen disposes its player entity. */
  public void captureFrom(Entity player) {
    CombatStatsComponent stats = requireStats(player);
    InventoryComponent inventory = requireInventory(player);
    restore(stats.getHealth(), stats.getMaxHealth(), inventory.getGold());
  }

  /** Replaces all persisted player values after validating them as one atomic state. */
  public void restore(int currentHealth, int maxHealth, int gold) {
    if (maxHealth <= 0) {
      throw new IllegalArgumentException("maxHealth must be positive");
    }
    if (currentHealth < 0 || currentHealth > maxHealth) {
      throw new IllegalArgumentException("currentHealth must be between 0 and maxHealth");
    }
    if (gold < 0) {
      throw new IllegalArgumentException("gold must not be negative");
    }

    this.currentHealth = currentHealth;
    this.maxHealth = maxHealth;
    this.gold = gold;
  }

  private CombatStatsComponent requireStats(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("player must have CombatStatsComponent");
    }
    return stats;
  }

  private InventoryComponent requireInventory(Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      throw new IllegalArgumentException("player must have InventoryComponent");
    }
    return inventory;
  }
}
