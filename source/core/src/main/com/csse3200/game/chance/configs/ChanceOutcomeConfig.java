package com.csse3200.game.chance.configs;

/** Configuration data for the health and currency effects of a Chance choice. */
public class ChanceOutcomeConfig {
  /** Required signed health change. */
  public Integer healthDelta;

  /** Required signed currency change. */
  public Integer currencyDelta;

  /** Required by the JSON deserialiser. */
  public ChanceOutcomeConfig() {}
}
