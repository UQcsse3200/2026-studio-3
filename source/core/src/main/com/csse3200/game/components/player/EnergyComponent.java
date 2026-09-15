package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Component for managing player energy and handling turn lifecycle triggers. */
public class EnergyComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(EnergyComponent.class);

  private static final String EVT_UPDATE_ENERGY = "updateEnergy";
  private static final String EVT_UPDATE_MAX_ENERGY = "updateMaxEnergy";

  private int currentEnergy;
  private int maxEnergy;

  public EnergyComponent(int maxEnergy) {
    this.maxEnergy = maxEnergy;
    this.currentEnergy = maxEnergy;
  }

  // --- Getters & Setters ---
  public int getCurrentEnergy() {
    return currentEnergy;
  }

  private void notifyEnergyChange() {
    if (entity != null) {
      entity.getEvents().trigger(EVT_UPDATE_ENERGY, this.currentEnergy, this.maxEnergy);
    }
  }

  public void setCurrentEnergy(int currentEnergy) {
    if (currentEnergy >= 0) {
      this.currentEnergy = Math.min(currentEnergy, this.maxEnergy);
    } else {
      this.currentEnergy = 0;
    }
    notifyEnergyChange();
  }

  public int getMaxEnergy() {
    return maxEnergy;
  }

  public void setMaxEnergy(int maxEnergy) {
    this.maxEnergy = Math.max(maxEnergy, 1);
    this.currentEnergy = Math.min(this.currentEnergy, this.maxEnergy);
    notifyEnergyChange();
  }

  // --- Team 5 (Card System) integration stubs ---
  /**
   * Checks whether the player currently has enough energy to cover the given cost. Read-only check,
   * does not modify state. Intended for Team 5 to validate whether a card is playable before
   * committing to spendEnergy().
   *
   * @param amount the energy cost to check against
   * @return true if currentEnergy >= amount, false otherwise
   */
  public boolean canAfford(int amount) {
    return currentEnergy >= amount;
  }

  /**
   * Attempts to spend the given amount of energy, e.g. when a card is played. Fails safely (no
   * state change) if the player does not have enough energy.
   *
   * @param amount the amount of energy to spend, must be non-negative
   * @return true if the energy was successfully spent, false if insufficient energy
   */
  public boolean spendEnergy(int amount) {
    if (amount < 0) {
      logger.warn("Attempted to spend negative energy: {}", amount);
      return false;
    }
    if (!canAfford(amount)) {
      logger.debug("Not enough energy: have {}, need {}", currentEnergy, amount);
      return false;
    }
    this.currentEnergy -= amount;
    notifyEnergyChange();
    return true;
  }

  /**
   * Restores the given amount of energy, capped at maxEnergy. Can be used for card effects or other
   * sources that grant energy mid-turn.
   *
   * @param amount the amount of energy to restore, must be non-negative
   */
  public void restoreEnergy(int amount) {
    if (amount < 0) {
      logger.warn("Attempted to restore negative energy: {}", amount);
      return;
    }
    this.currentEnergy = Math.min(currentEnergy + amount, maxEnergy);
    notifyEnergyChange();
  }

  // --- Team 3 lifecycle hooks ---
  /**
   * Called by the turn/battle system at the start of the player's turn. Resets currentEnergy back
   * to maxEnergy.
   */
  public void onTurnStart() {
    this.currentEnergy = maxEnergy;
    notifyEnergyChange();
  }

  public void onTurnEnd() {
    // Intentionally left as a stub pending Team 3 alignment on
    // whether any end-of-turn energy behavior is needed.
  }
}
