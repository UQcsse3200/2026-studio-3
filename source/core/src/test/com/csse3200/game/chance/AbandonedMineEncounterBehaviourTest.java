package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbandonedMineEncounterBehaviourTest {
  private ChanceEncounter mine() {
    return ChanceEncounterConfigLoader.loadEncounters().stream()
        .filter(event -> event.getId().equals("abandoned-mine"))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void shouldGiveOnlyGoldAtTheNoCurseBoundary() {
    TestMineRandom random = new TestMineRandom(0.5, 0);
    ChanceEncounterBehaviour behaviour =
        ChanceEncounterBehaviourFactory.create(mine(), random, TestCardService.withCards());
    assertInstanceOf(AbandonedMineEncounterBehaviour.class, behaviour);
    ChanceOutcome outcome = behaviour.resolveChoice("search-tunnels").getOutcome();
    assertEquals(60, outcome.getCurrencyDelta());
    assertEquals(0, outcome.getHealthDelta());
    assertTrue(outcome.getItemRewardIds().isEmpty());
    assertSame(outcome, behaviour.resolveChoice("search-tunnels").getOutcome());
    assertEquals(1, random.chanceDraws);
    assertEquals(0, random.itemDraws);
  }

  @Test
  void shouldSelectEveryEquallyWeightedCurseAndKeepItsOutcome() {
    List<String> ids =
        List.of("BRAND_OF_THE_DAMNED", "BLOOD_OATH_SIGIL", "SHATTERED_AEGIS", "ASHEN_EMBER");
    for (int index = 0; index < ids.size(); index++) {
      TestMineRandom random = new TestMineRandom(0.499, index);
      ChanceEncounterBehaviour behaviour =
          ChanceEncounterBehaviourFactory.create(mine(), random, TestCardService.withCards());
      ChanceOutcome outcome = behaviour.resolveChoice("search-tunnels").getOutcome();
      assertEquals(List.of(ids.get(index)), outcome.getItemRewardIds());
      assertEquals(60, outcome.getCurrencyDelta());
      assertEquals(0, outcome.getHealthDelta());
      assertSame(outcome, behaviour.resolveChoice("search-tunnels").getOutcome());
      assertEquals(1, random.chanceDraws);
      assertEquals(1, random.itemDraws);
    }
  }

  @Test
  void shouldUseConfiguredWeightsAndRewardRatherThanHardCodedBalance() {
    ChanceCurseRisk risk =
        new ChanceCurseRisk(
            1,
            List.of(
                new ChanceCurseRisk.WeightedItem("LUCKY_COIN", 1),
                new ChanceCurseRisk.WeightedItem("ENERGY_CRYSTAL", 3)));
    ChanceEncounter event =
        new ChanceEncounter(
            "abandoned-mine",
            "Mine",
            List.of(
                new ChanceChoice("search-tunnels", "Explore", new ChanceOutcome(0, 77), risk),
                new ChanceChoice("leave", "Leave", new ChanceOutcome(0, 0))));
    for (int roll = 0; roll < 4; roll++) {
      ChanceOutcome outcome =
          new AbandonedMineEncounterBehaviour(event, new TestMineRandom(0.99, roll))
              .resolveChoice("search-tunnels")
              .getOutcome();
      assertEquals(77, outcome.getCurrencyDelta());
      assertEquals(
          List.of(roll == 0 ? "LUCKY_COIN" : "ENERGY_CRYSTAL"), outcome.getItemRewardIds());
    }
  }

  @Test
  void shouldNotRollForLeaveInvalidChoicesOrChoiceQueries() {
    ChanceEncounter event = mine();
    TestMineRandom random = new TestMineRandom(0, 0);
    var behaviour = new AbandonedMineEncounterBehaviour(event, random);
    assertEquals(List.of("search-tunnels", "leave"), behaviour.getAvailableChoiceIds(event));
    assertTrue(behaviour.resolveChoice("leave").getOutcome().isNoEffect());
    assertEquals(
        ChanceBehaviourResult.Type.INVALID_CHOICE,
        behaviour.resolveChoice("claim-shattered-aegis").getType());
    assertEquals(
        ChanceBehaviourResult.Type.INVALID_CHOICE,
        behaviour.resolveChoice("take-ashen-ember").getType());
    assertEquals(
        ChanceBehaviourResult.Type.INVALID_CHOICE, behaviour.resolveChoice(null).getType());
    assertEquals(0, random.chanceDraws);
    assertEquals(0, random.itemDraws);
  }

  @Test
  void shouldDrawIndependentlyForNewBehavioursEvenWithSameDefinitionAndRandomSource() {
    ChanceEncounter event = mine();
    java.util.Random random =
        new java.util.Random() {
          private int calls;

          @Override
          public double nextDouble() {
            return calls++ == 0 ? 0.9 : 0.1;
          }

          @Override
          public int nextInt(int bound) {
            return 3;
          }
        };
    ChanceEncounterBehaviour first =
        ChanceEncounterBehaviourFactory.create(event, random, TestCardService.withCards());
    ChanceEncounterBehaviour second =
        ChanceEncounterBehaviourFactory.create(event, random, TestCardService.withCards());
    assertTrue(first.resolveChoice("search-tunnels").getOutcome().getItemRewardIds().isEmpty());
    assertEquals(
        List.of("ASHEN_EMBER"),
        second.resolveChoice("search-tunnels").getOutcome().getItemRewardIds());
    assertTrue(first.resolveChoice("search-tunnels").getOutcome().getItemRewardIds().isEmpty());
  }
}
