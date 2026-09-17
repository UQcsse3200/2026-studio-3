package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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

    battleUI.getEvents().trigger("endturn");

    verify(mockController).endPlayerTurn();
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

    battleUI.getEvents().trigger("playCard", "strike", "enemy");

    assertTrue(playedEvents.isEmpty());
    assertEquals(3, testPlayer.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(List.of("strike"), deck.getHand());
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
