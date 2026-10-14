package com.csse3200.game.chance;

import java.util.List;

/**
 * Represents the Player-independent result of resolving a Chance Encounter choice.
 *
 * <p>Health and currency changes are signed deltas. Positive values represent gains and negative
 * values represent losses. Card rewards are represented by stable Card Service identifiers.
 */
public final class ChanceOutcome {
  private final int healthDelta;
  private final int currencyDelta;
  private final List<String> cardRewardIds;

  /**
   * Creates a Chance Encounter outcome.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   */
  public ChanceOutcome(int healthDelta, int currencyDelta) {
    this(healthDelta, currencyDelta, List.of());
  }

  /**
   * Creates a Chance Encounter outcome with an optional card reward.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   * @param cardRewardId stable rewarded card identifier, or null for no card reward
   */
  public ChanceOutcome(int healthDelta, int currencyDelta, String cardRewardId) {
    this(healthDelta, currencyDelta, cardRewardId == null ? List.of() : List.of(cardRewardId));
  }

  private ChanceOutcome(int healthDelta, int currencyDelta, List<String> cardRewardIds) {
    this.healthDelta = healthDelta;
    this.currencyDelta = currencyDelta;
    this.cardRewardIds = List.copyOf(cardRewardIds);
  }

  /**
   * Creates a Chance Encounter outcome containing zero or more ordered card rewards.
   *
   * <p>Duplicate identifiers are retained because an encounter may intentionally grant two copies
   * of the same card.
   *
   * @param healthDelta signed change in health
   * @param currencyDelta signed change in currency
   * @param cardRewardIds stable rewarded card identifiers
   * @return outcome containing an immutable copy of the supplied rewards
   */
  public static ChanceOutcome withCardRewards(
      int healthDelta, int currencyDelta, List<String> cardRewardIds) {
    return new ChanceOutcome(healthDelta, currencyDelta, cardRewardIds);
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
    return cardRewardIds.isEmpty() ? null : cardRewardIds.get(0);
  }

  /**
   * Gets all ordered card reward identifiers.
   *
   * @return immutable card reward list, possibly empty and possibly containing duplicates
   */
  public List<String> getCardRewardIds() {
    return cardRewardIds;
  }

  /**
   * Checks whether this outcome changes neither health nor currency.
   *
   * @return true when both deltas are zero
   */
  public boolean isNoEffect() {
    return healthDelta == 0 && currencyDelta == 0 && cardRewardIds.isEmpty();
  }
}
