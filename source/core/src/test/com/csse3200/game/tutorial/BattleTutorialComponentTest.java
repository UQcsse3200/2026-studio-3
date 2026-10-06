package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.events.listeners.EventListener2;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BattleTutorialComponentTest {
  private Application previousApp;
  private final List<Runnable> queued = new ArrayList<>();
  private final BattleController battle = mock(BattleController.class);
  private final FakeView view = new FakeView();
  private final List<BattleTutorialController.Outcome> outcomes = new ArrayList<>();
  private final BattleTutorialComponent component =
      new BattleTutorialComponent(battle, view, outcomes::add);

  @BeforeEach
  void setUp() {
    previousApp = Gdx.app;
    Gdx.app = mock(Application.class);
    doAnswer(
            invocation -> {
              queued.add(invocation.getArgument(0));
              return null;
            })
        .when(Gdx.app)
        .postRunnable(any());
  }

  @AfterEach
  void restoreApp() {
    component.dispose();
    Gdx.app = previousApp;
  }

  @Test
  void wiresTextAndContinueButDoesNotStartOrMutateTheBattle() {
    component.create();
    assertEquals(BattleTutorialController.Step.HAND, view.last().step());
    view.continueAction.run();
    assertEquals(BattleTutorialController.Step.CARD_COST, view.last().step());
    for (int i = 0; i < 6; i++) {
      view.continueAction.run();
    }
    assertEquals(BattleTutorialController.Step.PLAY_A_CARD, view.last().step());
    assertEquals(BattleTutorialPromptContent.PLAY_A_CARD, view.last().text());
    assertFalse(view.last().canContinue());
    view.continueAction.run();
    assertEquals(BattleTutorialController.Step.PLAY_A_CARD, view.last().step());
    verify(battle, never()).start();
    verify(battle, never()).endPlayerTurn();
  }

  @Test
  void successfulPlayAndActualEndTurnAdvanceTheView() {
    component.create();
    for (int i = 0; i < 7; i++) {
      view.continueAction.run();
    }
    cardListener().handle("card", "enemy");
    assertEquals(BattleTutorialController.Step.END_TURN, view.last().step());
    phaseListener().handle(BattlePhase.PLAYER_TURN, BattlePhase.PLAYER_END);
    assertEquals(BattleTutorialController.Step.FREE_PLAY, view.last().step());
  }

  @Test
  void victoryDefersCompletionAndCleansUpBeforeNotifyingTheOwner() {
    component.create();
    endListener().handle(true);
    assertTrue(outcomes.isEmpty());
    verify(battle, never()).removeBattleEndListener(any());
    component.update();
    component.update();
    assertEquals(1, queued.size());
    assertTrue(outcomes.isEmpty());
    queued.get(0).run();
    queued.get(0).run();
    assertEquals(List.of(BattleTutorialController.Outcome.WON), outcomes);
    assertEquals(1, view.clearCount);
    verify(battle).removeBattleEndListener(any());
    verify(battle).removeCardPlayedListener(any());
    verify(battle).removePhaseChangeListener(any());
  }

  @Test
  void defeatIsReportedWithoutDecidingTheNextScreen() {
    component.create();
    endListener().handle(false);
    component.update();
    queued.get(0).run();
    assertEquals(List.of(BattleTutorialController.Outcome.LOST), outcomes);
  }

  @Test
  void exitButtonReportsCancellationOnce() {
    component.create();
    Runnable exit = view.exitAction;
    exit.run();
    exit.run();
    component.update();
    queued.get(0).run();
    assertEquals(List.of(BattleTutorialController.Outcome.CANCELLED), outcomes);
    exit.run();
    assertEquals(1, outcomes.size());
  }

  @Test
  void leavingTheScreenCancelsAnAlreadyQueuedTransition() {
    component.create();
    endListener().handle(true);
    component.update();
    component.dispose();
    component.dispose();
    queued.get(0).run();
    assertTrue(outcomes.isEmpty());
    assertEquals(1, view.clearCount);
  }

  @Test
  void disposedViewDoesNotReactToRetainedCallbacks() {
    component.create();
    Runnable next = view.continueAction;
    Runnable exit = view.exitAction;
    EventListener1<Boolean> end = endListener();
    int shown = view.prompts.size();
    component.dispose();
    next.run();
    exit.run();
    end.handle(true);
    component.update();
    assertEquals(shown, view.prompts.size());
    assertTrue(outcomes.isEmpty());
    assertTrue(queued.isEmpty());
    assertThrows(IllegalStateException.class, component::create);
  }

  @Test
  void duplicateCreationIsRejectedAndNoAppFallbackStillCompletes() {
    component.create();
    assertThrows(IllegalStateException.class, component::create);
    Gdx.app = null;
    endListener().handle(true);
    component.update();
    assertEquals(List.of(BattleTutorialController.Outcome.WON), outcomes);
  }

  @SuppressWarnings("unchecked")
  private EventListener2<String, String> cardListener() {
    ArgumentCaptor<EventListener2<String, String>> captor =
        ArgumentCaptor.forClass(EventListener2.class);
    verify(battle).addCardPlayedListener(captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private EventListener2<BattlePhase, BattlePhase> phaseListener() {
    ArgumentCaptor<EventListener2<BattlePhase, BattlePhase>> captor =
        ArgumentCaptor.forClass(EventListener2.class);
    verify(battle).addPhaseChangeListener(captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private EventListener1<Boolean> endListener() {
    ArgumentCaptor<EventListener1<Boolean>> captor = ArgumentCaptor.forClass(EventListener1.class);
    verify(battle).addBattleEndListener(captor.capture());
    return captor.getValue();
  }

  private static final class FakeView implements BattleTutorialView {
    private final List<BattleTutorialPrompt> prompts = new ArrayList<>();
    private Runnable continueAction;
    private Runnable exitAction;
    private int clearCount;

    @Override
    public void bindActions(Runnable continueAction, Runnable exitAction) {
      this.continueAction = continueAction;
      this.exitAction = exitAction;
    }

    @Override
    public void show(BattleTutorialPrompt prompt) {
      prompts.add(prompt);
    }

    @Override
    public void clear() {
      clearCount++;
      continueAction = null;
      exitAction = null;
    }

    private BattleTutorialPrompt last() {
      return prompts.get(prompts.size() - 1);
    }
  }
}
