package com.csse3200.game.chance;

import com.csse3200.game.encounters.integration.CardCatalogGateway;
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

  /**
   * Creates the initial encounters while validating configured rewards against the card catalog.
   *
   * @param cardCatalog authoritative card lookup boundary
   * @return read-only initial encounter definitions
   */
  public static List<ChanceEncounter> createInitialEncounters(CardCatalogGateway cardCatalog) {
    return ChanceEncounterConfigLoader.loadEncountersWithCatalog(cardCatalog);
  }

  private ChanceEncounterFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
