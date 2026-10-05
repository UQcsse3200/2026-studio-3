package com.csse3200.game.chance;

import java.util.List;
import java.util.Optional;

/**
 * Resolves player choices for one Chance Encounter without applying changes to player state.
 *
 * <p>Implementations may produce fixed or runtime outcomes. Runtime implementations can receive
 * dependencies such as a seeded random source through their constructors. A behaviour may also
 * report a delegated result when another flow will finish the encounter later.
 */
@FunctionalInterface
public interface ChanceEncounterBehaviour {
  /**
   * Resolves a selected choice into an encounter result.
   *
   * @param choiceId selected choice identifier
   * @return non-null result describing an outcome, staged continuation, invalid choice, or
   *     delegated flow
   */
  ChanceBehaviourResult resolveChoice(String choiceId);

  /**
   * Choice IDs currently available to the player; fixed encounters show their catalogue choices.
   */
  default List<String> getAvailableChoiceIds(ChanceEncounter encounter) {
    return encounter.getChoices().stream().map(ChanceChoice::getId).toList();
  }

  /** Short instruction displayed above the currently available choices. */
  default String getChoicePrompt() {
    return "CHOOSE YOUR RESPONSE";
  }

  /** Feedback shown after a choice advances the encounter without completing it. */
  default String getStageResultText() {
    return "The encounter continues. Choose your next move.";
  }

  /** Last authoritative dice roll, when this encounter uses dice. */
  default Optional<DiceRoll> getLastDiceRoll() {
    return Optional.empty();
  }
}
