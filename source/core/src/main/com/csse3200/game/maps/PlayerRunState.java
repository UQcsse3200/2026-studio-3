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
   * Returns the bonus available for the next claimed gold reward.
   *
   * <p>Lucky Coins are consumable reward items. Owning one or more coins makes a single 10% bonus
   * available; additional copies are retained for later gold rewards instead of stacking on the
   * same reward.
   *
   * @return the single-coin multiplier, or zero when no Lucky Coin is owned
   */
  public float getGoldBonusMultiplier() {
    return itemInventory.hasItem(ItemType.LUCKY_COIN) ? LUCKY_COIN_BONUS : 0f;
  }

  /**
   * Calculates the discount from the durable owned-item list.
   *
   * @return accumulated shop discount, capped at 50%
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
   * Calculates the extra gold one Lucky Coin would add to a generated reward.
   *
   * <p>The bonus is 10% of the player's balance after adding the base reward, capped at 20. This
   * method does not mutate the balance or consume the coin, so reward UI can preview the exact
   * amount that {@link #claimGoldReward(int, boolean)} will grant.
   *
   * @param amount generated base reward
   * @return Lucky Coin bonus, or zero when no Lucky Coin is owned
   */
  public int calculateLuckyCoinBonus(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    if (!itemInventory.hasItem(ItemType.LUCKY_COIN)) {
      return 0;
    }

    int subtotal = gold + amount;
    return Math.min(Math.round(subtotal * LUCKY_COIN_BONUS), MAX_LUCKY_COIN_GOLD_BONUS);
  }

  /**
   * Claims a generated gold reward and optionally consumes the Lucky Coin used to boost it.
   *
   * @param amount final generated gold amount
   * @param useLuckyCoin whether this option was generated with a Lucky Coin bonus
   */
  public void claimGoldReward(int amount, boolean useLuckyCoin) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    if (useLuckyCoin && !itemInventory.hasItem(ItemType.LUCKY_COIN)) {
      throw new IllegalStateException("cannot consume a Lucky Coin that is not owned");
    }

    int subtotal = gold + amount;
    int luckyCoinBonus = useLuckyCoin ? calculateLuckyCoinBonus(amount) : 0;
    gold = subtotal + luckyCoinBonus;
    if (useLuckyCoin) {
      itemInventory.removeItem(ItemType.LUCKY_COIN);
    }
  }

  public void addOwnedItem(ItemType itemId) {
    itemInventory.addItem(itemId);
  }

  /** Removes one copy of an owned item. */
  public boolean removeOwnedItem(ItemType itemId) {
    return itemInventory.removeItem(itemId);
  }

  /** Returns whether the player owns at least one copy of an item. */
  public boolean hasOwnedItem(ItemType itemId) {
    return itemInventory.hasItem(itemId);
  }

  /** Returns the number of copies owned for an item. */
  public int getOwnedItemCount(ItemType itemId) {
    return itemInventory.getItemCount(itemId);
  }

  public List<ItemType> getOwnedItems() {
    return itemInventory.getItems();
  }

  /**
   * Uses one owned battle consumable on the current player entity.
   *
   * <p>The item is removed only after its effect is applied successfully.
   *
   * @param itemId battle item to use
   * @param player current battle player
   * @return true when one copy was used; false when the item is not usable or not owned
   */
  public boolean useBattleItem(ItemType itemId, Entity player) {
    if (itemId == null || !itemId.isBattleConsumable() || !itemInventory.hasItem(itemId)) {
      return false;
    }

    requireStats(player);
    ItemEffectApplier.applyItemEffect(itemId, player);
    return itemInventory.removeItem(itemId);
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

    for (ItemType itemId : itemInventory.getItems()) {
      // Lucky Coin is consumed only when the player claims a boosted gold reward. It has no
      // always-on effect on the battle entity.
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
