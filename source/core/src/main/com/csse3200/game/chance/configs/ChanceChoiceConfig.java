package com.csse3200.game.chance.configs;

/** Configuration data for one selectable Chance Encounter choice. */
public class ChanceChoiceConfig {
  /** Stable choice identifier within its encounter. */
  public String id = "";

  /** Player-facing choice description. */
  public String description = "";

  /** Outcome produced by selecting this choice. */
  public ChanceOutcomeConfig outcome = new ChanceOutcomeConfig();

  /** Required by the JSON deserialiser. */
  public ChanceChoiceConfig() {}
}
