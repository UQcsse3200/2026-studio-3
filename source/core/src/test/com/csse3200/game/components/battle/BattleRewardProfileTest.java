package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.CardRewardProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BattleRewardProfileTest {
  @Test
  void capturesNormalEliteAndFinalProfilesBeforeClearingTheActiveNode() {
    assertTransition(RoomType.COMBAT, true, false, CardRewardProfile.NORMAL);
    assertTransition(RoomType.ELITE, true, false, CardRewardProfile.ELITE);
    assertTransition(RoomType.COMBAT, true, false, CardRewardProfile.NORMAL);
    assertTransition(RoomType.FINAL, true, false, CardRewardProfile.ELITE);
  }

  @Test
  void defeatUsesNormalAndEliteRarityDoesNotDependOnTempleEligibility() {
    assertTransition(RoomType.ELITE, false, false, CardRewardProfile.NORMAL);
  }

  @Test
  void tutorialKeepsItsOwnCompletionFlowInsteadOfOpeningPostBattleRewards() {
    GdxGame game = mock(GdxGame.class);
    BattleController controller = mock(BattleController.class);
    Entity entity = new Entity().addComponent(new BattleActions(controller, game, true));
    Application original = Gdx.app;
    Gdx.app = null;
    try {
      entity.create();
      @SuppressWarnings("unchecked")
      ArgumentCaptor<EventListener1<Boolean>> listener =
          ArgumentCaptor.forClass(EventListener1.class);
      verify(controller).addBattleEndListener(listener.capture());
      listener.getValue().handle(true);
      verify(game).setScreen(GdxGame.ScreenType.TUTORIAL_VICTORY);
      verify(game, never()).showBattleResult(anyBoolean(), any());
      verify(game, never()).requestAutosaveAfterEncounter();
    } finally {
      entity.getComponent(BattleActions.class).dispose();
      Gdx.app = original;
    }
  }

  private void assertTransition(
      RoomType room, boolean won, boolean tutorial, CardRewardProfile expected) {
    Application original = Gdx.app;
    Application application = mock(Application.class);
    Gdx.app = application;
    try {
      RunState run = new RunState();
      run.restoreRun(new MapGraph(Map.of(7, new MapNode(7, room)), false), 7);
      run.setPlayerMaxHealth(100);
      run.setPlayerHealth(10); // Elite rarity must still apply when the 80% Temple gate is false.
      GdxGame game = mock(GdxGame.class);
      when(game.getRunState()).thenReturn(run);
      BattleController controller = mock(BattleController.class);
      Entity entity = new Entity().addComponent(new BattleActions(controller, game, tutorial));
      entity.create();
      @SuppressWarnings("unchecked")
      ArgumentCaptor<EventListener1<Boolean>> listener =
          ArgumentCaptor.forClass(EventListener1.class);
      verify(controller).addBattleEndListener(listener.capture());
      listener.getValue().handle(won);
      assertNull(run.getActiveNodeId());
      assertFalse(run.hasPendingEliteTempleReward());
      assertEquals(won && room == RoomType.FINAL, run.isFinalEncounterCompleted());
      ArgumentCaptor<Runnable> transition = ArgumentCaptor.forClass(Runnable.class);
      verify(application).postRunnable(transition.capture());
      verify(game, never()).showBattleResult(anyBoolean(), any());
      transition.getValue().run();
      verify(game).showBattleResult(won, expected);
      entity.getComponent(BattleActions.class).dispose();
    } finally {
      Gdx.app = original;
    }
  }
}
