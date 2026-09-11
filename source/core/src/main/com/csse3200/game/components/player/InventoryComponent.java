package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Currently only stores the gold amount but can be extended for more advanced functionality such
 * as storing items. Can also be used as a more generic component for other entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);
  private int gold;
  private float goldBonusMultiplier = 0f;
  private float shopDiscount = 0f;

  public InventoryComponent(int gold) {
    setGold(gold);
  }

  /**
   * Returns the player's gold.
   *
   * @return entity's gold
   */
  public int getGold() {
    return this.gold;
  }

  /**
   * Returns if the player has a certain amount of gold.
   *
   * @param gold required amount of gold
   * @return player has greater than or equal to the required amount of gold
   */
  public Boolean hasGold(int gold) {
    return this.gold >= gold;
  }

  /**
   * Sets the player's gold. Gold has a minimum bound of 0.
   *
   * @param gold gold
   */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);

    if (entity != null) {
      entity.getEvents().trigger("updateGold", this.gold);
    }
  }

  /**
   * Adds to the player's gold. The amount added can be negative.
   *
   * @param gold gold to add
   */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }

  /**
   * Subtracts a specific amount of gold from the player's inventory. Checks if the player has
   * enough gold before subtracting.
   *
   * @param amount the amount of gold to subtract
   * @return true if successful, false if not enough gold or invalid amount
   */
  public boolean subtractGold(int amount) {
    if (amount < 0) {
      return false;
    }

    if (!hasGold(amount)) {
      return false;
    }

    setGold(this.gold - amount);
    return true;
  }

  public void addShopDiscount(float amount) {
    this.shopDiscount = Math.min(this.shopDiscount + amount, 0.5f);
  }

  public float getShopDiscount() {
    return shopDiscount;
  }

  public float getGoldBonusMultiplier() {
    return goldBonusMultiplier;
  }

  public void addGoldBonusMultiplier(float bonus) {
    this.goldBonusMultiplier += bonus;
  }
}

