package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.csse3200.game.cards.TestCardService;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChanceEncounterBehaviourFactoryTest {
  @Test
  void shouldCreateWishingFountainBehaviourForItsDefinition() {
    ChanceEncounterBehaviour behaviour =
        ChanceEncounterBehaviourFactory.create(
            wishingFountainEncounter(), new Random(266L), TestCardService.withCards("bandage"));

    assertInstanceOf(WishingFountainEncounterBehaviour.class, behaviour);
  }

  @Test
  void shouldCreateDiceBehaviourForDiceDefinition() {
    ChanceEncounterBehaviour behaviour =
        ChanceEncounterBehaviourFactory.create(
            new ChanceEncounter(
                DiceEncounterBehaviour.ENCOUNTER_ID,
                "A dice keeper offers a wager.",
                List.of(new ChanceChoice("low", "Predict low.", new ChanceOutcome(0, 0)))),
            new Random(266L),
            TestCardService.withCards("bandage"));

    assertInstanceOf(DiceEncounterBehaviour.class, behaviour);
  }

  @Test
  void shouldCreateCardFusionBehaviourForCardFusionDefinition() {
    ChanceEncounterBehaviour behaviour =
        ChanceEncounterBehaviourFactory.create(
            new ChanceEncounter(
                CardFusionEncounterBehaviour.ENCOUNTER_ID,
                "A forge offers to fuse cards.",
                List.of(
                    new ChanceChoice(
                        CardFusionEncounterBehaviour.FUSE_CHOICE_ID,
                        "Fuse.",
                        new ChanceOutcome(0, 0)))),
            new Random(266L),
            TestCardService.withCards());

    assertInstanceOf(CardFusionEncounterBehaviour.class, behaviour);
    assertEquals(
        ChanceBehaviourResult.Type.DELEGATED,
        behaviour.resolveChoice(CardFusionEncounterBehaviour.FUSE_CHOICE_ID).getType());
  }

  @Test
  void shouldPreserveFixedBehaviourForEveryRetainedOrdinaryEncounter() {
    for (String encounterId :
        List.of("mysterious-shrine", "wandering-healer", "flooded-crossing", "abandoned-mine")) {
      ChanceOutcome configuredOutcome = new ChanceOutcome(-10, 25);
      ChanceEncounter encounter =
          new ChanceEncounter(
              encounterId,
              "Ordinary event.",
              List.of(new ChanceChoice("offer", "Make an offering.", configuredOutcome)));
      ChanceEncounterBehaviour behaviour =
          ChanceEncounterBehaviourFactory.create(
              encounter, new Random(266L), TestCardService.withCards());

      ChanceBehaviourResult result = behaviour.resolveChoice("offer");

      assertInstanceOf(FixedChanceEncounterBehaviour.class, behaviour);
      assertEquals(ChanceBehaviourResult.Type.OUTCOME, result.getType());
      assertSame(configuredOutcome, result.getOutcome());
    }
  }

  private static ChanceEncounter wishingFountainEncounter() {
    return new ChanceEncounter(
        WishingFountainEncounterBehaviour.ENCOUNTER_ID,
        "An old wishing fountain.",
        List.of(
            new ChanceChoice(
                WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID,
                "Make a wish.",
                new ChanceOutcome(0, 0)),
            new ChanceChoice(
                WishingFountainEncounterBehaviour.LEAVE_CHOICE_ID,
                "Leave.",
                new ChanceOutcome(0, 0))));
  }
}
