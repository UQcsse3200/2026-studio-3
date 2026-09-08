package com.csse3200.game.chance;

/**
 * Represents the Player-independent result of resolving a Chance Encounter choice.
 *
 * <p>Health and currency changes are signed deltas. Positive values represent gains, negative
 * values represent losses, and an outcome with both deltas set to zero has no effect.
 */
public final class ChanceOutcome {
  private final int healthDelta;
  private final int currencyDelta;

  /**
   * Creates a Chance Encounter outcome.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   */
  public ChanceOutcome(int healthDelta, int currencyDelta) {
    this.healthDelta = healthDelta;
    this.currencyDelta = currencyDelta;
  }

  /**
   * Gets the signed health change.
   *
   * @return health delta
   */
  public int getHealthDelta() {
    return healthDelta;
  }

  /**
   * Gets the signed currency change.
   *
   * @return currency delta
   */
  public int getCurrencyDelta() {
    return currencyDelta;
  }

  /**
   * Checks whether this outcome changes neither health nor currency.
   *
   * @return true when both deltas are zero
   */
  public boolean isNoEffect() {
    return healthDelta == 0 && currencyDelta == 0;
  }
}
