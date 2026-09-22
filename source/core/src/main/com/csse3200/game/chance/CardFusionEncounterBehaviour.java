package com.csse3200.game.chance;

/** Routes the Card Fusion Event from its introductory choice to the Fusion flow. */
public final class CardFusionEncounterBehaviour implements ChanceEncounterBehaviour {
  /** Stable identifier reserved for the Card Fusion Event catalogue entry. */
  public static final String ENCOUNTER_ID = "card-fusion";

  /** Stable identifier of the choice that opens the Card Fusion flow. */
  public static final String FUSE_CHOICE_ID = "fuse";

  @Override
  public ChanceBehaviourResult resolveChoice(String choiceId) {
    if (!FUSE_CHOICE_ID.equals(choiceId)) {
      return ChanceBehaviourResult.invalidChoice();
    }
    return ChanceBehaviourResult.delegated();
  }
}
