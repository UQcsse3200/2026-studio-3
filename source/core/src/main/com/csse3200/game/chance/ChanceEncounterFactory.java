package com.csse3200.game.chance;

import java.util.List;

/** Creates the initial Sprint 1 Chance Encounter definitions. */
public final class ChanceEncounterFactory {

  /**
   * Creates the initial Chance Encounters in deterministic order.
   *
   * @return read-only initial encounter definitions
   */
  public static List<ChanceEncounter> createInitialEncounters() {
    return List.of(createMysteriousShrine(), createHealingSpring(), createForgottenCache());
  }

  private static ChanceEncounter createMysteriousShrine() {
    return new ChanceEncounter(
        "mysterious-shrine",
        "An ancient shrine hums with an unsettling energy.",
        List.of(
            new ChanceChoice(
                "make-offering", "Offer some of your vitality.", new ChanceOutcome(-10, 25)),
            new ChanceChoice("leave", "Leave the shrine untouched.", new ChanceOutcome(0, 0))));
  }

  private static ChanceEncounter createHealingSpring() {
    return new ChanceEncounter(
        "healing-spring",
        "A clear spring glows softly beside the path.",
        List.of(
            new ChanceChoice("drink", "Drink from the spring.", new ChanceOutcome(15, 0)),
            new ChanceChoice("leave", "Continue without drinking.", new ChanceOutcome(0, 0))));
  }

  private static ChanceEncounter createForgottenCache() {
    return new ChanceEncounter(
        "forgotten-cache",
        "You discover an abandoned cache hidden beneath loose stones.",
        List.of(
            new ChanceChoice(
                "take-coins", "Take the coins from the cache.", new ChanceOutcome(0, 15)),
            new ChanceChoice("leave", "Leave the cache untouched.", new ChanceOutcome(0, 0))));
  }

  private ChanceEncounterFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
