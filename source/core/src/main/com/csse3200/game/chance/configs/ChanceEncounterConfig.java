package com.csse3200.game.chance.configs;

/** Configuration data for one Chance Encounter definition. */
public class ChanceEncounterConfig {
  /** Stable encounter identifier. */
  public String id = "";

  /** Player-facing encounter description. */
  public String description = "";

  /** Required positive selection weight. */
  public Integer weight;

  /** Ordered choices available for this encounter. */
  public ChanceChoiceConfig[] choices = new ChanceChoiceConfig[0];

  /** Required by the JSON deserialiser. */
  public ChanceEncounterConfig() {}
}
