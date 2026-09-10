package com.csse3200.game.chance;

import java.util.List;

/** Provides the initial Chance Encounter definitions loaded from configuration. */
public final class ChanceEncounterFactory {

  /**
   * Creates the initial Chance Encounters in deterministic order.
   *
   * @return read-only initial encounter definitions
   */
  public static List<ChanceEncounter> createInitialEncounters() {
    return ChanceEncounterConfigLoader.loadEncounters();
  }

  private ChanceEncounterFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
