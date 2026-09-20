package com.csse3200.game.chance.configs;

/** Root configuration object for Chance Encounter definitions. */
public class ChanceConfig {
  /** Chance Encounters available to the game, in configuration order. */
  public ChanceEncounterConfig[] encounters = new ChanceEncounterConfig[0];

  /** Required by the JSON deserialiser. */
  public ChanceConfig() {}
}
