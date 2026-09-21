package com.csse3200.game.chance;

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
}
