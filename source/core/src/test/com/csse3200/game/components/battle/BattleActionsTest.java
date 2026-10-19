package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.utils.Timer;
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
import com.csse3200.game.cards.play.CardPlayRequest;
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
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

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
  void shouldAutoEndTurnAfterOneSecondWithoutEnergy() {
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    controller.start();
    energy.spendEnergy(3);

    when(time.getDeltaTime()).thenReturn(0.5f);
    entity.update();
    assertEquals(1, controller.getPlayerTurnNumber());
    assertEquals(20, player.getComponent(CombatStatsComponent.class).getHealth());

    entity.update();
    assertEquals(2, controller.getPlayerTurnNumber());
    assertEquals(19, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void shouldKeepTheTurnOpenWhileEnergyRemains() {
    player.addComponent(new EnergyComponent(3));
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(2f);
    controller.start();

    entity.update();

    assertEquals(1, controller.getPlayerTurnNumber());
    assertEquals(20, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldRestartCountdownAfterEnergyIsRestoredAndSpentAgain() {
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    controller.start();
    energy.spendEnergy(3);
    when(time.getDeltaTime()).thenReturn(0.75f);
    entity.update();

    energy.restoreEnergy(1);
    entity.update();
    assertEquals(1, controller.getPlayerTurnNumber());

    energy.spendEnergy(1);
    when(time.getDeltaTime()).thenReturn(0.5f);
    entity.update();
    assertEquals(1, controller.getPlayerTurnNumber());
    entity.update();
    assertEquals(2, controller.getPlayerTurnNumber());
  }

  @Test
  void shouldPauseTheCountdownWithGameplay() {
    Graphics previousGraphics = Gdx.graphics;
    try {
      Gdx.graphics = mock(Graphics.class);
      when(Gdx.graphics.getDeltaTime()).thenReturn(0.5f);
      GameTime time = new GameTime();
      ServiceLocator.registerTimeSource(time);
      GamePauseService pause = new GamePauseService(time);
      EnergyComponent energy = new EnergyComponent(3);
      player.addComponent(energy);
      controller.start();
      energy.spendEnergy(3);
      entity.update();

      pause.pause();
      when(Gdx.graphics.getDeltaTime()).thenReturn(2f);
      entity.update();
      assertEquals(1, controller.getPlayerTurnNumber());

      pause.resume();
      when(Gdx.graphics.getDeltaTime()).thenReturn(0.5f);
      entity.update();
      assertEquals(2, controller.getPlayerTurnNumber());
    } finally {
      Gdx.graphics = previousGraphics;
    }
  }

  @Test
  void shouldResetCountdownOnManualEndAndWaitForEnemyReveals() {
    List<Timer.Task> reveals = new ArrayList<>();
    try (MockedStatic<Timer> timer = mockStatic(Timer.class)) {
      timer
          .when(() -> Timer.schedule(any(Timer.Task.class), anyFloat()))
          .thenAnswer(
              invocation -> {
                Timer.Task task = invocation.getArgument(0);
                reveals.add(task);
                return task;
              });
      EnergyComponent energy = new EnergyComponent(3);
      player.addComponent(energy);
      GameTime time = mock(GameTime.class);
      ServiceLocator.registerTimeSource(time);
      controller.start();
      energy.spendEnergy(3);
      when(time.getDeltaTime()).thenReturn(0.75f);
      entity.update();

      entity.getEvents().trigger("endTurn");
      assertEquals(2, controller.getPlayerTurnNumber());
      energy.spendEnergy(3);
      when(time.getDeltaTime()).thenReturn(2f);
      entity.update();
      assertEquals(2, controller.getPlayerTurnNumber());

      reveals.get(0).run();
      when(time.getDeltaTime()).thenReturn(0.5f);
      entity.update();
      assertEquals(2, controller.getPlayerTurnNumber());
      entity.update();
      assertEquals(3, controller.getPlayerTurnNumber());
    }
  }

  @Test
  void shouldAutoEndAfterAPlayedCardSpendsTheLastEnergy() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
    assertTrue(
        battle.controller().submitCardPlayRequest(CardPlayRequest.self(battle.instanceId())));
    assertEquals(0, battle.energy().getCurrentEnergy());
    assertEquals(1, battle.controller().getPlayerTurnNumber());

    battle.ui().update();
    assertEquals(1, battle.controller().getPlayerTurnNumber());
    battle.ui().update();
    assertEquals(2, battle.controller().getPlayerTurnNumber());
  }

  @Test
  void shouldKeepTurnOpenWhenTheLastEnergyCardRestoresEnergy() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.ENERGY_GAIN, 2));
    assertTrue(
        battle.controller().submitCardPlayRequest(CardPlayRequest.self(battle.instanceId())));
    assertEquals(2, battle.energy().getCurrentEnergy());

    for (int frame = 0; frame < 4; frame++) {
      battle.ui().update();
    }
    assertEquals(1, battle.controller().getPlayerTurnNumber());
  }

  private CardBattle cardBattle(EffectConfig effect) {
    CardConfig config = card("last_energy", CardType.SKILL, TargetType.SELF, effect);
    config.cost = 3;
    CardLibrary library = new CardLibrary(List.of(config));
    BattleDeck deck =
        new BattleDeck(new PlayerDeck(TestCardService.withCards(config.id), List.of(config.id)));
    deck.drawCards(1);
    EnergyComponent energy = new EnergyComponent(3);
    Entity testPlayer =
        new Entity().addComponent(new CombatStatsComponent(20, 0)).addComponent(energy);
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    BattleController battle =
        new BattleController(
            testPlayer,
            List.of(enemy),
            new CardEffectHandler(),
            new CardPlayService(library, deck, energy));
    Entity ui = new Entity().addComponent(new BattleActions(battle, mock(GdxGame.class)));
    ui.create();
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.5f);
    ServiceLocator.registerTimeSource(time);
    battle.start();
    return new CardBattle(battle, ui, energy, deck.getHand().get(0).instanceId());
  }

  private record CardBattle(
      BattleController controller, Entity ui, EnergyComponent energy, String instanceId) {}

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
  void tutorialKeepsTheBattleActionsButDoesNotInstallOrdinaryOutcomeNavigation() {
    BattleController mockController = mock(BattleController.class);
    GdxGame game = mock(GdxGame.class);
    Entity battleUI = new Entity().addComponent(new BattleActions(mockController, game, true));
    battleUI.create();

    battleUI.getEvents().trigger("endTurn");

    verify(mockController).endPlayerTurn();
    verify(mockController, never()).addBattleEndListener(any());
    verify(game, never()).requestAutosaveAfterEncounter();
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
