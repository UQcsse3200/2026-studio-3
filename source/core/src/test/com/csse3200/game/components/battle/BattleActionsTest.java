package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattleEvent;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BattleActionsTest {
  private BattleController controller;
  private Entity player;
  private Entity entity;

  @BeforeEach
  void setUp() {
    player = new Entity();
    player.addComponent(new CombatStatsComponent(20, 0));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    controller = new BattleController(player, List.of(enemy));
    GdxGame game = mock(GdxGame.class);
    entity = new Entity().addComponent(new BattleActions(controller, game));
    entity.create();
  }

  private static CardConfig card(String id, CardType type, TargetType target, EffectConfig effect) {
    CardConfig config = new CardConfig();
    config.id = id;
    config.name = id;
    config.cost = 1;
    config.type = type;
    config.target = target;
    config.effects = new EffectConfig[] {effect};
    config.texturePath = "images/cards/" + id + ".png";
    return config;
  }

  @Test
  void shouldNotSubmitUnknownCard() {
    BattleController mockController = mock(BattleController.class);
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();

    battleUI.getEvents().trigger("playCard", "missing", "bone_crawler");

    verify(mockController, never()).submitCardPlayRequest(any());
  }

  @Test
  void shouldNotFireCardPlayedWhenControllerRejectsRequest() {
    BattleController mockController = mock(BattleController.class);
    when(mockController.submitCardPlayRequest(any())).thenReturn(false);
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();
    List<String> playedEvents = new ArrayList<>();
    battleUI
        .getEvents()
        .addListener(
            "cardPlayed",
            (String cardName, String targetId) -> playedEvents.add(cardName + ":" + targetId));

    battleUI.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertTrue(playedEvents.isEmpty());
  }

  @Test
  void shouldEndTurnWhenProductionEndTurnEventFires() {
    BattleController mockController = mock(BattleController.class);
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();

    battleUI.getEvents().trigger("endTurn");

    verify(mockController).endPlayerTurn();
  }

  @Test
  void tutorialKeepsBattleActionsAndInstallsItsOutcomeNavigation() {
    BattleController mockController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    Entity battleUI = new Entity().addComponent(new BattleActions(mockController, game, true));
    battleUI.create();

    battleUI.getEvents().trigger("endTurn");

    verify(mockController).endPlayerTurn();
    verify(mockController).addBattleEndListener(any());
    verify(game, never()).requestAutosaveAfterEncounter();
  }

  @Test
  void tutorialVictoryWaitsForEveryEnemyReleaseWithoutChangingTheNormalRun() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    RunState runState = mock(RunState.class);
    when(game.getRunState()).thenReturn(runState);
    EnemyReleaseComponent first = mock(EnemyReleaseComponent.class);
    EnemyReleaseComponent second = mock(EnemyReleaseComponent.class);
    Entity battleUI =
        new Entity()
            .addComponent(
                new BattleActions(
                    battleController,
                    game,
                    List.of(new Entity().addComponent(first), new Entity().addComponent(second)),
                    true));
    battleUI.create();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());

    Application previousApp = Gdx.app;
    Application app = mock(Application.class);
    List<Runnable> navigation = new ArrayList<>();
    doAnswer(invocation -> navigation.add(invocation.getArgument(0))).when(app).postRunnable(any());
    Gdx.app = app;
    try {
      endListener.getValue().handle(true);
      verify(first).startRelease();
      verify(second).startRelease();
      battleUI.update();
      assertTrue(navigation.isEmpty());

      when(first.isFinished()).thenReturn(true);
      battleUI.update();
      assertTrue(navigation.isEmpty());

      when(second.isFinished()).thenReturn(true);
      battleUI.update();
      battleUI.update();
      assertEquals(1, navigation.size());
      verify(game, never()).setScreen(any(GdxGame.ScreenType.class));
      navigation.get(0).run();
      verify(game).setScreen(GdxGame.ScreenType.TUTORIAL_VICTORY);
      verify(game, never()).requestAutosaveAfterEncounter();
      verify(game, never()).startNewRun();
      verifyNoInteractions(runState);
    } finally {
      Gdx.app = previousApp;
    }
  }

  @Test
  void tutorialDefeatReturnsToTheMenuWithoutReleasingEnemiesOrChangingTheRun() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    EnemyReleaseComponent release = mock(EnemyReleaseComponent.class);
    Entity battleUI =
        new Entity()
            .addComponent(
                new BattleActions(
                    battleController, game, List.of(new Entity().addComponent(release)), true));
    battleUI.create();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());

    Application previousApp = Gdx.app;
    Gdx.app = null;
    try {
      endListener.getValue().handle(false);
      verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
      verify(game, never()).getRunState();
      verify(game, never()).startNewRun();
      verify(release, never()).startRelease();
    } finally {
      Gdx.app = previousApp;
    }
  }

  @Test
  void ordinaryVictoryStillWaitsForReleaseAndOpensTheRewardScreen() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    RunState runState = mock(RunState.class);
    when(game.getRunState()).thenReturn(runState);
    when(runState.getActiveNodeId()).thenReturn(7);
    EnemyReleaseComponent release = mock(EnemyReleaseComponent.class);
    Entity battleUI =
        new Entity()
            .addComponent(
                new BattleActions(
                    battleController, game, List.of(new Entity().addComponent(release))));
    battleUI.create();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());

    Application previousApp = Gdx.app;
    Gdx.app = null;
    try {
      endListener.getValue().handle(true);
      verify(release).startRelease();
      verify(runState).completeEncounter(true);
      verify(game).requestAutosaveAfterEncounter();
      battleUI.update();
      verify(game, never()).setScreen(any(GdxGame.ScreenType.class));

      when(release.isFinished()).thenReturn(true);
      battleUI.update();
      verify(game).setScreen(GdxGame.ScreenType.VICTORY);
    } finally {
      Gdx.app = previousApp;
    }
  }

  @Test
  void disposedTutorialDoesNotRunQueuedVictoryNavigation() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    Entity battleUI = new Entity().addComponent(new BattleActions(battleController, game, true));
    battleUI.create();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());

    Application previousApp = Gdx.app;
    Application app = mock(Application.class);
    ArgumentCaptor<Runnable> navigation = ArgumentCaptor.forClass(Runnable.class);
    Gdx.app = app;
    try {
      endListener.getValue().handle(true);
      verify(app).postRunnable(navigation.capture());
      battleUI.getComponent(BattleActions.class).dispose();
      navigation.getValue().run();
      verifyNoInteractions(game);
    } finally {
      Gdx.app = previousApp;
    }
  }

  @Test
  void shouldNotFireCardPlayedWhenCardSystemRejectsRequest() {
    CardConfig expensiveStrike =
        card(
            "strike",
            CardType.ATTACK,
            TargetType.SINGLE_ENEMY,
            new EffectConfig(EffectType.DAMAGE, 6));
    expensiveStrike.cost = 4;
    CardLibrary library = new CardLibrary(List.of(expensiveStrike));
    BattleDeck deck =
        new BattleDeck(
            new PlayerDeck(
                TestCardService.withCards("strike", "bandage"), List.of("strike", "bandage")));
    deck.drawCards(1);
    String strikeInstanceId = deck.getHand().get(0).instanceId();
    Entity testPlayer =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    CardEffectHandler effectHandler = new CardEffectHandler();
    CardPlayService cardPlayService =
        new CardPlayService(library, deck, testPlayer.getComponent(EnergyComponent.class));
    BattleController realController =
        new BattleController(testPlayer, List.of(enemy), effectHandler, cardPlayService);
    Entity battleUI =
        new Entity().addComponent(new BattleActions(realController, mock(GdxGame.class)));
    battleUI.create();
    List<String> playedEvents = new ArrayList<>();
    battleUI
        .getEvents()
        .addListener(
            "cardPlayed",
            (String cardName, String targetId) -> playedEvents.add(cardName + ":" + targetId));
    realController.start();

    battleUI.getEvents().trigger("playCard", strikeInstanceId, "enemy");

    assertTrue(playedEvents.isEmpty());
    assertEquals(3, testPlayer.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(strikeInstanceId, deck.getHand().get(0).instanceId());
  }

  @Test
  void shouldRunEnemyPhaseWhenEndTurnSelected() {
    advanceToPlayerTurn();

    entity.getEvents().trigger("endTurnSelected");

    // End turn runs the enemy phase (enemy attacks for its base attack of 1) and hands control
    // back.
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(19, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldIgnoreSelectionWhenCurrentPhaseCannotHandleIt() {
    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  @Test
  void shouldRequestAutosaveAfterWinningAnActiveEncounter() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    RunState runState = mock(RunState.class);
    when(game.getRunState()).thenReturn(runState);
    when(runState.getActiveNodeId()).thenReturn(7);
    new Entity().addComponent(new BattleActions(battleController, game)).create();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());
    endListener.getValue().handle(true);

    verify(runState).completeEncounter(true);
    verify(game).requestAutosaveAfterEncounter();
  }

  @Test
  void shouldNotRequestAutosaveAfterDefeat() {
    BattleController battleController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    RunState runState = mock(RunState.class);
    when(game.getRunState()).thenReturn(runState);
    when(runState.getActiveNodeId()).thenReturn(7);
    new Entity().addComponent(new BattleActions(battleController, game)).create();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<Boolean>> endListener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(battleController).addBattleEndListener(endListener.capture());
    endListener.getValue().handle(false);

    verify(runState).completeEncounter(false);
    verify(game, never()).requestAutosaveAfterEncounter();
  }

  private void advanceToPlayerTurn() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
  }
}
