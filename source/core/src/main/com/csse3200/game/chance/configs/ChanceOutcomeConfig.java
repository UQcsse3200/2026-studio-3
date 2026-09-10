package com.csse3200.game.chance.configs;

/** Configuration data for the health and currency effects of a Chance choice. */
public class ChanceOutcomeConfig {
  /** Signed health change. */
  public int healthDelta = 0;

  /** Signed currency change. */
  public int currencyDelta = 0;

  /** Required by the JSON deserialiser. */
  public ChanceOutcomeConfig() {}
}
