package com.csse3200.game.chance.configs;

/** Configuration data for the health, currency, and optional card effects of a Chance choice. */
public class ChanceOutcomeConfig {
  /** Required signed health change. */
  public Integer healthDelta;

  /** Required signed currency change. */
  public Integer currencyDelta;

  /** Optional stable Card Service identifier rewarded by this outcome. */
  public String cardRewardId;

  /** Required by the JSON deserialiser. */
  public ChanceOutcomeConfig() {}
}
