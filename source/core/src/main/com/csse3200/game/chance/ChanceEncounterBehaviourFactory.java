package com.csse3200.game.chance;

import com.csse3200.game.cards.CardService;
import java.util.Objects;
import java.util.Random;

/** Creates the runtime behaviour associated with a selected Chance Encounter definition. */
public final class ChanceEncounterBehaviourFactory {
  /**
   * Creates specialised behaviour where one exists and fixed behaviour otherwise.
   *
   * @param encounter selected encounter definition
   * @param random injected source for runtime-random behaviour
   * @param cardService authoritative source of registered cards
   * @return behaviour for the selected encounter
   */
  public static ChanceEncounterBehaviour create(
      ChanceEncounter encounter, Random random, CardService cardService) {
    Objects.requireNonNull(encounter, "encounter cannot be null");
    Objects.requireNonNull(random, "random cannot be null");
    Objects.requireNonNull(cardService, "cardService cannot be null");

    if (SpringEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId())) {
      return new SpringEncounterBehaviour(random, cardService);
    }
    if (DiceEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId())) {
      return new DiceEncounterBehaviour(random, cardService);
    }
    return new FixedChanceEncounterBehaviour(encounter);
  }

  private ChanceEncounterBehaviourFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
