package com.csse3200.game.chance;

import java.util.Objects;

/** Preserves the original fixed-choice resolution behaviour of a {@link ChanceEncounter}. */
public final class FixedChanceEncounterBehaviour implements ChanceEncounterBehaviour {
  private final ChanceEncounter encounter;

  /**
   * Creates a behaviour backed by the encounter's predefined choices and outcomes.
   *
   * @param encounter fixed Chance Encounter definition
   */
  public FixedChanceEncounterBehaviour(ChanceEncounter encounter) {
    this.encounter = Objects.requireNonNull(encounter, "encounter cannot be null");
  }

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    ChanceOutcome outcome = encounter.resolveChoice(choiceId);
    if (outcome == null) {
      return ChanceBehaviourResult.invalidChoice();
    }
    return ChanceBehaviourResult.outcome(outcome);
  }
}
