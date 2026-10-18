package com.csse3200.game.chance.configs;

/** Configuration data for one selectable Chance Encounter choice. */
public class ChanceChoiceConfig {
  /** Stable choice identifier within its encounter. */
  public String id = "";

  /** Player-facing choice description. */
  public String description = "";

  /** Required base outcome, before runtime risk is resolved. */
  public ChanceOutcomeConfig outcome;

  /** Optional runtime Curse risk, currently supported only by the Mine exploration choice. */
  public ChanceCurseRiskConfig curseRisk;

  /** Required by the JSON deserialiser. */
  public ChanceChoiceConfig() {
    // Default constructor required for LibGDX JSON deserialisation.
  }
}
