package com.csse3200.game.maps;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.ItemEffectApplier;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import java.util.List;

/** Run-scoped player values that must survive screen disposal. */
public class PlayerRunState {
  private static final float LUCKY_COIN_BONUS = 0.1f;
  private static final float MERCHANTS_FAVOR_DISCOUNT = 0.05f;
  private static final float MAX_SHOP_DISCOUNT = 0.5f;

  private int currentHealth;
  private int maxHealth;
  private int gold;
  private final List<ItemType> ownedItems = new ArrayList<>();

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

  /**
   * Calculates the bonus from the durable owned-item list.
   *
   * @return accumulated Lucky Coin multiplier
   */
  public float getGoldBonusMultiplier() {
    long luckyCoinCount = ownedItems.stream().filter(item -> item == ItemType.LUCKY_COIN).count();
    return luckyCoinCount * LUCKY_COIN_BONUS;
  }

  /**
   * Calculates the discount from the durable owned-item list.
   *
   * @return accumulated shop discount, capped at 50%
   */
  public float getShopDiscount() {
    long favourCount = ownedItems.stream().filter(item -> item == ItemType.MERCHANTS_FAVOR).count();
    return Math.min(favourCount * MERCHANTS_FAVOR_DISCOUNT, MAX_SHOP_DISCOUNT);
  }

  public void addGold(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    gold += amount;
  }

  public void addOwnedItem(ItemType itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("itemId must not be null");
    }
    ownedItems.add(itemId);
  }

  public List<ItemType> getOwnedItems() {
    return List.copyOf(ownedItems);
  }

  /**
   * Applies durable state to a newly-created player entity.
   *
   * <p>This method must be called exactly once for each newly-created player entity.
   */
  public void applyTo(Entity player) {
    CombatStatsComponent stats = requireStats(player);
    InventoryComponent inventory = requireInventory(player);

    stats.setMaxHealth(maxHealth);
    stats.setHealth(currentHealth);
    inventory.setGold(gold);

    // Reconstruct derived values from the durable item list.
    inventory.setGoldBonusMultiplier(0f);
    inventory.setShopDiscount(0f);

    for (ItemType itemId : ownedItems) {
      ItemEffectApplier.applyItemEffect(itemId, player);
    }
  }

  /** Captures mutable base values before a gameplay entity is disposed. */
  public void captureFrom(Entity player) {
    CombatStatsComponent stats = requireStats(player);
    InventoryComponent inventory = requireInventory(player);

    restore(stats.getHealth(), stats.getMaxHealth(), inventory.getGold());
  }

  /**
   * Replaces health and gold while retaining the current owned-item list.
   *
   * @param currentHealth current player health
   * @param maxHealth maximum player health
   * @param gold current player gold
   */
  public void restore(int currentHealth, int maxHealth, int gold) {
    validateState(currentHealth, maxHealth, gold);

    this.currentHealth = currentHealth;
    this.maxHealth = maxHealth;
    this.gold = gold;
  }

  private void validateState(int currentHealth, int maxHealth, int gold) {
    if (maxHealth <= 0) {
      throw new IllegalArgumentException("maxHealth must be positive");
    }
    if (currentHealth < 0 || currentHealth > maxHealth) {
      throw new IllegalArgumentException("currentHealth must be between 0 and maxHealth");
    }
    if (gold < 0) {
      throw new IllegalArgumentException("gold must not be negative");
    }
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
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    if (inventory == null) {
      throw new IllegalArgumentException("player must have InventoryComponent");
    }
    return inventory;
  }
}
