package com.csse3200.game.maps;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.ItemEffectApplier;
import com.csse3200.game.rewards.ItemInventory;
import com.csse3200.game.rewards.ItemType;
import java.util.List;

/** Run-scoped player values that must survive screen disposal. */
public class PlayerRunState {
  private static final float LUCKY_COIN_BONUS = 0.1f;
  private static final int MAX_LUCKY_COIN_GOLD_BONUS = 20;
  private static final float MERCHANTS_FAVOR_DISCOUNT = 0.10f;
  private static final float MAX_SHOP_DISCOUNT = 0.5f;

  private int currentHealth;
  private int maxHealth;
  private int gold;

  private final ItemInventory itemInventory = new ItemInventory();

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
   * Returns the bonus multiplier available for the next claimed gold reward.
   *
   * <p>Lucky Coins do not stack on the same reward. Additional copies are retained
   * for subsequent gold rewards.
   *
   * @return 10% when at least one Lucky Coin is owned, otherwise zero
   */
  public float getGoldBonusMultiplier() {
    return itemInventory.hasItem(ItemType.LUCKY_COIN) ? LUCKY_COIN_BONUS : 0f;
  }

  /**
   * Calculates the accumulated shop discount, capped at 50%.
   *
   * @return the current shop discount
   */
  public float getShopDiscount() {
    return Math.min(
            itemInventory.getItemCount(ItemType.MERCHANTS_FAVOR) * MERCHANTS_FAVOR_DISCOUNT,
            MAX_SHOP_DISCOUNT);
  }

  public void addGold(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    gold += amount;
  }

  /**
   * Calculates the extra gold a Lucky Coin would add to a generated reward.
   *
   * <p>The bonus is 10% of the balance after adding the base reward, capped at 20.
   * This method does not mutate the balance or consume the coin.
   *
   * @param amount generated base reward
   * @return the bonus gold, or zero if no Lucky Coin is owned
   */
  public int calculateLuckyCoinBonus(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    if (!itemInventory.hasItem(ItemType.LUCKY_COIN)) {
      return 0;
    }

    int subtotal = gold + amount;
    return Math.min(
            Math.round(subtotal * LUCKY_COIN_BONUS),
            MAX_LUCKY_COIN_GOLD_BONUS);
  }

  /**
   * Claims a gold reward and optionally consumes one Lucky Coin.
   *
   * @param amount generated base reward
   * @param useLuckyCoin whether to use a Lucky Coin for this reward
   */
  public void claimGoldReward(int amount, boolean useLuckyCoin) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    if (useLuckyCoin && !itemInventory.hasItem(ItemType.LUCKY_COIN)) {
      throw new IllegalStateException("cannot consume a Lucky Coin that is not owned");
    }

    int subtotal = gold + amount;
    int bonus = useLuckyCoin ? calculateLuckyCoinBonus(amount) : 0;

    gold = subtotal + bonus;

    if (useLuckyCoin) {
      itemInventory.removeItem(ItemType.LUCKY_COIN);
    }
  }

  public void addOwnedItem(ItemType itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("itemId must not be null");
    }
    itemInventory.addItem(itemId);
  }

  /** Removes one copy of an owned item. */
  public boolean removeOwnedItem(ItemType itemId) {
    return itemInventory.removeItem(itemId);
  }

  /** Returns whether at least one copy of an item is owned. */
  public boolean hasOwnedItem(ItemType itemId) {
    return itemInventory.hasItem(itemId);
  }

  /** Returns the number of copies of an item currently owned. */
  public int getOwnedItemCount(ItemType itemId) {
    return itemInventory.getItemCount(itemId);
  }

  /** Returns a snapshot of the currently owned items. */
  public List<ItemType> getOwnedItems() {
    return List.copyOf(itemInventory.getItems());
  }

  /**
   * Replaces the durable item list after validating all loaded values.
   *
   * @param items replacement items
   */
  public void replaceOwnedItems(List<ItemType> items) {
    if (items == null || items.stream().anyMatch(item -> item == null)) {
      throw new IllegalArgumentException("items must not be null or contain null");
    }

    // Remove existing copies without modifying the collection being iterated.
    for (ItemType item : List.copyOf(itemInventory.getItems())) {
      itemInventory.removeItem(item);
    }

    for (ItemType item : items) {
      itemInventory.addItem(item);
    }
  }

  /**
   * Uses one owned battle consumable on the current player entity.
   *
   * <p>The item is removed only after its effect has been applied successfully.
   *
   * @param itemId battle item to use
   * @param player current battle player
   * @return true if one item was used; false if it was unusable or not owned
   */
  public boolean useBattleItem(ItemType itemId, Entity player) {
    if (itemId == null
            || !itemId.isBattleConsumable()
            || !itemInventory.hasItem(itemId)) {
      return false;
    }

    requireStats(player);
    ItemEffectApplier.applyItemEffect(itemId, player);
    return itemInventory.removeItem(itemId);
  }

  /**
   * Applies durable state to a newly created player entity.
   *
   * <p>Call exactly once for each newly created player entity.
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

    for (ItemType itemId : itemInventory.getItems()) {
      // Lucky Coins are consumed through gold rewards, not applied as permanent effects.
      if (itemId != ItemType.LUCKY_COIN && !itemId.isBattleConsumable()) {
        ItemEffectApplier.applyItemEffect(itemId, player);
      }
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
      throw new IllegalArgumentException(
              "currentHealth must be between 0 and maxHealth");
    }
    if (gold < 0) {
      throw new IllegalArgumentException("gold must not be negative");
    }
  }

  private CombatStatsComponent requireStats(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }

    CombatStatsComponent stats =
            player.getComponent(CombatStatsComponent.class);

    if (stats == null) {
      throw new IllegalArgumentException(
              "player must have CombatStatsComponent");
    }
    return stats;
  }

  private InventoryComponent requireInventory(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }

    InventoryComponent inventory =
            player.getComponent(InventoryComponent.class);

    if (inventory == null) {
      throw new IllegalArgumentException(
              "player must have InventoryComponent");
    }
    return inventory;
  }
}
