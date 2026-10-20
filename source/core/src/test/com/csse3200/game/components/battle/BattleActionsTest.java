package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
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
  private MockedStatic<Timer> timer;
  private final List<Timer.Task> enemyReveals = new ArrayList<>();

  @BeforeEach
  void setUp() {
    timer = mockStatic(Timer.class);
    timer
        .when(() -> Timer.schedule(any(Timer.Task.class), anyFloat()))
        .thenAnswer(
            invocation -> {
              Timer.Task task = invocation.getArgument(0);
              enemyReveals.add(task);
              return task;
            });
    player = new Entity();
    player.addComponent(new CombatStatsComponent(20, 0));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    controller = new BattleController(player, List.of(enemy));
    GdxGame game = mock(GdxGame.class);
    entity =
        new Entity()
            .addComponent(new BattleActions(controller, game))
            .addComponent(new CardActions());
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

  @AfterEach
  void tearDown() {
    timer.close();
  }

  @Test
  void shouldEndAnEmptyPlayerTurnWithoutWaiting() {
    EnergyComponent energy = new EnergyComponent(3);
    player.addComponent(energy);
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    controller.start();
    energy.spendEnergy(3);

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
    when(time.getDeltaTime()).thenReturn(6f);
    controller.start();

    entity.update();

    assertEquals(1, controller.getPlayerTurnNumber());
    assertEquals(20, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldAutoEndAfterAPlayedCardSpendsTheLastEnergy() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
    assertTrue(
        battle.controller().submitCardPlayRequest(CardPlayRequest.self(battle.instanceId())));
    assertEquals(0, battle.energy().getCurrentEnergy());
    when(ServiceLocator.getTimeSource().getDeltaTime()).thenReturn(0f);

    battle.ui().update();

    assertEquals(2, battle.controller().getPlayerTurnNumber());
  }

  @Test
  void shouldWaitOneAndAHalfSecondsAfterEnemyEffectsBeforeReturningTheHand() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
    AtomicBoolean handVisible = observeHand(battle.ui());
    List<String> reveals = new ArrayList<>();
    battle
        .ui()
        .getEvents()
        .addListener(
            BattleActions.BATTLE_LOG_EVENT, (String message) -> reveals.add("enemyAction"));
    battle
        .ui()
        .getEvents()
        .addListener("playerTurnStarted", (Integer turn) -> reveals.add("playerTurn"));
    BattleActions actions = battle.ui().getComponent(BattleActions.class);
    when(ServiceLocator.getTimeSource().getDeltaTime()).thenReturn(0.75f);

    battle.ui().getEvents().trigger("endTurn");
    assertFalse(handVisible.get());
    assertTrue(actions.hasPendingEnemyReveals());
    assertFalse(actions.isPlayerTurn());
    battle.ui().update(); // The pause must not start before the enemy action is shown.
    enemyReveals.get(0).run();
    assertTrue(reveals.contains("enemyAction"));
    assertFalse(reveals.contains("playerTurn"));
    assertFalse(handVisible.get());

    battle.ui().update(); // 0.75 seconds after the enemy action.
    assertFalse(handVisible.get());
    assertTrue(actions.hasPendingEnemyReveals());
    battle.ui().update(); // 1.5 seconds after the enemy action.
    assertTrue(handVisible.get());
    assertEquals("playerTurn", reveals.get(reveals.size() - 1));
    assertFalse(actions.hasPendingEnemyReveals());
    assertTrue(actions.isPlayerTurn());
  }

  @Test
  void shouldPauseTheDelayAfterTheEnemyTurnWithGameplay() {
    Graphics previousGraphics = Gdx.graphics;
    try {
      Gdx.graphics = mock(Graphics.class);
      when(Gdx.graphics.getDeltaTime()).thenReturn(0.75f);
      CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
      AtomicBoolean handVisible = observeHand(battle.ui());
      GameTime time = new GameTime();
      ServiceLocator.registerTimeSource(time);
      GamePauseService pause = new GamePauseService(time);
      battle.ui().getEvents().trigger("endTurn");
      enemyReveals.get(0).run();
      battle.ui().update();
      assertFalse(handVisible.get());

      pause.pause();
      when(Gdx.graphics.getDeltaTime()).thenReturn(6f);
      battle.ui().update();
      assertFalse(handVisible.get());

      pause.resume();
      when(Gdx.graphics.getDeltaTime()).thenReturn(0.75f);
      battle.ui().update();
      assertTrue(handVisible.get());
    } finally {
      Gdx.graphics = previousGraphics;
    }
  }

  @Test
  void shouldIgnoreEndTurnPressesWhileEnemyPresentationIsPending() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
    battle.ui().getEvents().trigger("endTurn");
    battle.ui().getEvents().trigger("endTurn");
    battle.ui().getEvents().trigger("endTurnSelected");
    assertEquals(2, battle.controller().getPlayerTurnNumber());

    enemyReveals.get(0).run();
    battle.ui().getEvents().trigger("endTurn");
    battle.ui().getEvents().trigger("endTurnSelected");
    assertEquals(2, battle.controller().getPlayerTurnNumber());

    battle.ui().update();
    battle.ui().update();
    battle.ui().getEvents().trigger("endTurn");
    assertEquals(3, battle.controller().getPlayerTurnNumber());
  }

  @Test
  void shouldCancelThePendingHandReturnWhenDisposed() {
    CardBattle battle = cardBattle(new EffectConfig(EffectType.BLOCK, 1));
    AtomicBoolean handVisible = observeHand(battle.ui());
    battle.ui().getEvents().trigger("endTurn");
    enemyReveals.get(0).run();
    battle.ui().getComponent(BattleActions.class).dispose();
    battle.ui().update();
    battle.ui().update();
    assertFalse(handVisible.get());
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

  private AtomicBoolean observeHand(Entity ui) {
    AtomicBoolean visible = new AtomicBoolean(true);
    ui.getEvents().addListener("up", () -> visible.set(true));
    ui.getEvents().addListener("down", () -> visible.set(false));
    return visible;
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
    Entity ui =
        new Entity()
            .addComponent(new BattleActions(battle, mock(GdxGame.class)))
            .addComponent(new CardActions());
    ui.create();
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(1.5f);
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
