package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.events.listeners.EventListener2;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BattleTutorialControllerTest {
  @Test
  void referenceSequenceRequiresTheExactDemonstrationCopyAtStepSix() {
    BattleTutorialController tutorial = new BattleTutorialController(true);
    tutorial.setDemonstrationCardId("demo-strike");
    tutorial.observeInitialHealth(32, 100);
    tutorial.start();
    assertEquals(BattleTutorialController.Step.HAND, tutorial.getCurrentStep().orElseThrow());
    tutorial.onSuccessfulCardPlay("demo-strike");
    for (int i = 0; i < 4; i++) assertTrue(tutorial.continueInformation());
    assertEquals(
        BattleTutorialController.Step.PLAY_A_CARD, tutorial.getCurrentStep().orElseThrow());
    assertFalse(tutorial.continueInformation());
    tutorial.onSuccessfulCardPlay("other-card");
    assertEquals(
        BattleTutorialController.Step.PLAY_A_CARD, tutorial.getCurrentStep().orElseThrow());
    tutorial.onSuccessfulCardPlay("demo-strike");
    assertEquals(
        BattleTutorialController.Step.CARD_ANIMATION, tutorial.getCurrentStep().orElseThrow());
    tutorial.observeResolution(32, 100, true, BattlePhase.PLAYER_TURN);
    assertEquals(
        BattleTutorialController.Step.CARD_ANIMATION, tutorial.getCurrentStep().orElseThrow());
    tutorial.observeResolution(26, 100, false, BattlePhase.PLAYER_TURN);
    assertEquals(
        BattleTutorialController.Step.CARD_ANIMATION, tutorial.getCurrentStep().orElseThrow());
    tutorial.observeResolution(26, 100, true, BattlePhase.PLAYER_TURN);
    assertEquals(
        BattleTutorialController.Step.ENEMY_STATS, tutorial.getCurrentStep().orElseThrow());
    tutorial.onSuccessfulCardPlay("demo-strike");
    assertEquals(
        BattleTutorialController.Step.ENEMY_STATS, tutorial.getCurrentStep().orElseThrow());
    assertTrue(tutorial.continueInformation());
    assertEquals(BattleTutorialController.Step.USED_CARD, tutorial.getCurrentStep().orElseThrow());
    assertTrue(tutorial.continueInformation());
    assertEquals(BattleTutorialController.Step.END_TURN, tutorial.getCurrentStep().orElseThrow());
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);
    assertEquals(
        BattleTutorialController.Step.ENEMY_ANIMATION, tutorial.getCurrentStep().orElseThrow());
    tutorial.observeResolution(26, 100, true, BattlePhase.PLAYER_TURN);
    assertEquals(BattleTutorialController.Step.HEALTH, tutorial.getCurrentStep().orElseThrow());
    assertTrue(tutorial.continueInformation());
    assertEquals(BattleTutorialController.Step.FREE_PLAY, tutorial.getCurrentStep().orElseThrow());
    assertTrue(tutorial.getOutcome().isEmpty());
  }

  @Test
  void endTurnExplanationCanBeSkippedWithoutEndingARealTurn() {
    var tutorial = new BattleTutorialController(true);
    tutorial.setDemonstrationCardId("strike");
    tutorial.observeInitialHealth(32, 100);
    tutorial.start();
    for (int i = 0; i < 4; i++) tutorial.continueInformation();
    tutorial.onSuccessfulCardPlay("strike");
    tutorial.observeResolution(28, 100, true, BattlePhase.PLAYER_TURN);
    tutorial.continueInformation();
    tutorial.continueInformation();
    assertTrue(tutorial.continueInformation());
    assertEquals(BattleTutorialController.Step.HEALTH, tutorial.getCurrentStep().orElseThrow());
  }

  @Test
  void advancesInformationThenRequiresRealActions() {
    BattleTutorialController tutorial = new BattleTutorialController();
    List<BattleTutorialController.Step> shown = new ArrayList<>();
    tutorial.addStepListener(shown::add);

    assertTrue(tutorial.start());
    assertFalse(tutorial.start());
    for (int i = 0; i < 7; i++) {
      assertTrue(tutorial.continueInformation());
    }
    assertEquals(
        BattleTutorialController.Step.PLAY_A_CARD, tutorial.getCurrentStep().orElseThrow());
    assertFalse(tutorial.continueInformation());

    tutorial.onPhaseChanged(BattlePhase.PLAYER_TURN);
    assertEquals(
        BattleTutorialController.Step.PLAY_A_CARD, tutorial.getCurrentStep().orElseThrow());
    tutorial.onSuccessfulCardPlay();
    assertEquals(BattleTutorialController.Step.END_TURN, tutorial.getCurrentStep().orElseThrow());
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);
    assertEquals(BattleTutorialController.Step.FREE_PLAY, tutorial.getCurrentStep().orElseThrow());
    assertFalse(tutorial.continueInformation());
    assertEquals(
        List.of(
            BattleTutorialController.Step.HAND,
            BattleTutorialController.Step.CARD_COST,
            BattleTutorialController.Step.ENERGY,
            BattleTutorialController.Step.HEALTH,
            BattleTutorialController.Step.BUFFS,
            BattleTutorialController.Step.CARD_DRAW,
            BattleTutorialController.Step.BATTLE_OUTCOME_RULES,
            BattleTutorialController.Step.PLAY_A_CARD,
            BattleTutorialController.Step.END_TURN,
            BattleTutorialController.Step.FREE_PLAY),
        shown);
  }

  @Test
  void actionsPerformedDuringInformationDoNotLeaveGuideStuck() {
    BattleTutorialController tutorial = new BattleTutorialController();
    tutorial.start();
    tutorial.onSuccessfulCardPlay();
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);

    for (int i = 0; i < 7; i++) {
      tutorial.continueInformation();
    }

    assertEquals(BattleTutorialController.Step.FREE_PLAY, tutorial.getCurrentStep().orElseThrow());
  }

  @Test
  void battleCanEndEarlyWithoutRepeatedOutcome() {
    BattleTutorialController tutorial = new BattleTutorialController();
    List<BattleTutorialController.Outcome> outcomes = new ArrayList<>();
    tutorial.addOutcomeListener(outcomes::add);
    tutorial.start();

    tutorial.onBattleEnded(false);
    tutorial.onBattleEnded(true);
    tutorial.onSuccessfulCardPlay();
    tutorial.cancel();

    assertEquals(
        BattleTutorialController.Step.BATTLE_ENDED, tutorial.getCurrentStep().orElseThrow());
    assertEquals(List.of(BattleTutorialController.Outcome.LOST), outcomes);
    assertFalse(tutorial.continueInformation());
  }

  @Test
  void cancelStopsFurtherProgress() {
    BattleTutorialController tutorial = new BattleTutorialController();
    tutorial.start();
    tutorial.cancel();
    tutorial.onBattleEnded(true);

    assertEquals(BattleTutorialController.Step.CANCELLED, tutorial.getCurrentStep().orElseThrow());
    assertEquals(BattleTutorialController.Outcome.CANCELLED, tutorial.getOutcome().orElseThrow());
    assertFalse(tutorial.continueInformation());
  }

  @Test
  void bindsToBattleOnlyOnce() {
    BattleTutorialController tutorial = new BattleTutorialController();
    BattleController battle = mock(BattleController.class);

    tutorial.bindTo(battle);

    verify(battle, times(1)).addCardPlayedListener(any());
    verify(battle, times(1)).addPhaseChangeListener(any());
    verify(battle, times(1)).addBattleEndListener(any());
    assertThrows(IllegalStateException.class, () -> tutorial.bindTo(battle));
    assertThrows(NullPointerException.class, () -> new BattleTutorialController().bindTo(null));
  }

  @Test
  void boundBattleEventsDriveActionStepsAndOutcome() {
    BattleTutorialController tutorial = new BattleTutorialController();
    BattleController battle = mock(BattleController.class);
    tutorial.bindTo(battle);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener2<String, String>> cardListener =
        ArgumentCaptor.forClass(EventListener2.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener2<BattlePhase, BattlePhase>> phaseListener =
        ArgumentCaptor.forClass(EventListener2.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battle).addCardPlayedListener(cardListener.capture());
    verify(battle).addPhaseChangeListener(phaseListener.capture());
    verify(battle).addBattleEndListener(endListener.capture());

    tutorial.start();
    for (int i = 0; i < 7; i++) {
      tutorial.continueInformation();
    }
    cardListener.getValue().handle("card-1", "enemy-1");
    assertEquals(BattleTutorialController.Step.END_TURN, tutorial.getCurrentStep().orElseThrow());
    phaseListener.getValue().handle(BattlePhase.PLAYER_TURN, BattlePhase.PLAYER_END);
    assertEquals(BattleTutorialController.Step.FREE_PLAY, tutorial.getCurrentStep().orElseThrow());
    endListener.getValue().handle(true);
    assertEquals(BattleTutorialController.Outcome.WON, tutorial.getOutcome().orElseThrow());
  }

  @Test
  void endTurnBeforePlayingDoesNotSatisfyTheLaterAction() {
    BattleTutorialController tutorial = new BattleTutorialController();
    tutorial.start();
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);
    tutorial.onSuccessfulCardPlay();
    for (int i = 0; i < 7; i++) {
      tutorial.continueInformation();
    }

    assertEquals(BattleTutorialController.Step.END_TURN, tutorial.getCurrentStep().orElseThrow());
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);
    assertEquals(BattleTutorialController.Step.FREE_PLAY, tutorial.getCurrentStep().orElseThrow());
  }

  @Test
  void ignoresEventsBeforeStartAndNullOutcome() {
    BattleTutorialController tutorial = new BattleTutorialController();
    tutorial.onSuccessfulCardPlay();
    tutorial.onPhaseChanged(BattlePhase.PLAYER_END);
    tutorial.onBattleEnded(true);
    tutorial.cancel();
    assertTrue(tutorial.getOutcome().isEmpty());
    assertFalse(tutorial.continueInformation());
    tutorial.start();
    tutorial.onBattleEnded(null);
    assertTrue(tutorial.getOutcome().isEmpty());
  }

  @Test
  void closeReleasesExactBattleListenersAndIsSilentAndIdempotent() {
    BattleController battle = mock(BattleController.class);
    BattleTutorialController tutorial = new BattleTutorialController();
    List<BattleTutorialController.Outcome> outcomes = new ArrayList<>();
    tutorial.addOutcomeListener(outcomes::add);
    tutorial.bindTo(battle);
    tutorial.start();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener2<String, String>> cards =
        ArgumentCaptor.forClass(EventListener2.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener2<BattlePhase, BattlePhase>> phases =
        ArgumentCaptor.forClass(EventListener2.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> ends = ArgumentCaptor.forClass(EventListener1.class);
    verify(battle).addCardPlayedListener(cards.capture());
    verify(battle).addPhaseChangeListener(phases.capture());
    verify(battle).addBattleEndListener(ends.capture());

    tutorial.close();
    tutorial.close();
    verify(battle, times(1)).removeCardPlayedListener(cards.getValue());
    verify(battle, times(1)).removePhaseChangeListener(phases.getValue());
    verify(battle, times(1)).removeBattleEndListener(ends.getValue());
    cards.getValue().handle("card", "enemy");
    phases.getValue().handle(BattlePhase.PLAYER_TURN, BattlePhase.PLAYER_END);
    ends.getValue().handle(true);
    tutorial.cancel();
    assertTrue(outcomes.isEmpty());
    assertFalse(tutorial.start());
    assertFalse(tutorial.continueInformation());
    assertThrows(IllegalStateException.class, () -> tutorial.bindTo(battle));
    assertThrows(IllegalStateException.class, () -> tutorial.addStepListener(step -> {}));
    assertThrows(IllegalStateException.class, () -> tutorial.addOutcomeListener(outcome -> {}));
  }

  @Test
  void canDisposeAnUnstartedGuideWithoutNotifyingTheOwner() {
    BattleTutorialController tutorial = new BattleTutorialController();
    tutorial.close();
    assertFalse(tutorial.start());
    assertTrue(tutorial.getOutcome().isEmpty());
  }
}
