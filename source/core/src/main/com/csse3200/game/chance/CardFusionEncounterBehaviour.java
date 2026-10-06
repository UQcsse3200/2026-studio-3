package com.csse3200.game.chance;

/** Routes the Card Fusion Event from its introductory choice to the Fusion flow. */
public final class CardFusionEncounterBehaviour implements ChanceEncounterBehaviour {
  /** Stable identifier reserved for the Card Fusion Event catalogue entry. */
  public static final String ENCOUNTER_ID = "card-fusion";

  /** Stable identifier of the choice that opens the Card Fusion flow. */
  public static final String FUSE_CHOICE_ID = "fuse";

  /** Stable identifier of the choice that leaves before entering the Fusion flow. */
  public static final String LEAVE_CHOICE_ID = "leave";

  private static final ChanceOutcome NO_EFFECT = new ChanceOutcome(0, 0);

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    if (FUSE_CHOICE_ID.equals(choiceId)) {
      return ChanceBehaviourResult.delegated();
    }
    if (LEAVE_CHOICE_ID.equals(choiceId)) {
      return ChanceBehaviourResult.outcome(NO_EFFECT);
    }
    return ChanceBehaviourResult.invalidChoice();
  }
}
