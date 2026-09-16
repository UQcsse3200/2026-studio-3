package com.csse3200.game.chance;

/**
 * Represents the Player-independent result of resolving a Chance Encounter choice.
 *
 * <p>Health and currency changes are signed deltas. Positive values represent gains and negative
 * values represent losses. A card reward is represented by its stable Card Service identifier.
 */
public final class ChanceOutcome {
  private final int healthDelta;
  private final int currencyDelta;
  private final String cardRewardId;

  /**
   * Creates a Chance Encounter outcome.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   */
  public ChanceOutcome(int healthDelta, int currencyDelta) {
    this(healthDelta, currencyDelta, null);
  }

  /**
   * Creates a Chance Encounter outcome with an optional card reward.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   * @param cardRewardId stable rewarded card identifier, or null for no card reward
   */
  public ChanceOutcome(int healthDelta, int currencyDelta, String cardRewardId) {
    this.healthDelta = healthDelta;
    this.currencyDelta = currencyDelta;
    this.cardRewardId = cardRewardId;
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
   * Gets the stable identifier of the rewarded card.
   *
   * @return rewarded card identifier, or null when this outcome grants no card
   */
  public String getCardRewardId() {
    return cardRewardId;
  }

  /**
   * Checks whether this outcome changes neither health nor currency.
   *
   * @return true when both deltas are zero
   */
  public boolean isNoEffect() {
    return healthDelta == 0 && currencyDelta == 0 && cardRewardId == null;
  }
}
