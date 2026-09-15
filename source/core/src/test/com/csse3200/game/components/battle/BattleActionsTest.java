package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattleEvent;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleActionsTest {
  private BattleController controller;
  private Entity player;
  private Entity entity;
  private String enemyId;

  @BeforeEach
  void setUp() {
    player = new Entity();
    player.addComponent(new CombatStatsComponent(20, 0));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    player.addComponent(new EnergyComponent(3));
    CardLibrary library = realLibrary();
    BattleDeck deck =
        new BattleDeck(
            PlayerDeck.fromInstances(
                library,
                List.of(
                    new CardInstance("strike-instance", "strike", 0),
                    new CardInstance("defend-instance", "defend", 0),
                    new CardInstance("bandage-instance", "bandage", 0))));
    deck.drawCards(3);
    controller =
        new BattleController(
            player, List.of(enemy), new CardEffectResolver(library), library, deck);
    enemyId = Integer.toString(enemy.getId());
    GdxGame game = mock(GdxGame.class);
    entity = new Entity().addComponent(new BattleActions(controller, game));
    entity.create();
  }

  private static BattleController mockControllerWithCards() {
    BattleController mockController = mock(BattleController.class);
    for (String cardId : List.of("strike", "defend", "bandage")) {
      when(mockController.resolveCardInHand(cardId + "-instance"))
          .thenReturn(
              Optional.of(
                  new CardResolver()
                      .resolve(
                          realLibrary().getCard(cardId).orElseThrow(),
                          new CardInstance(cardId + "-instance", cardId, 0))));
    }
    return mockController;
  }

  private static CardLibrary realLibrary() {
    return new CardLibrary(
        List.of(
            card(
                "strike",
                CardType.ATTACK,
                TargetType.SINGLE_ENEMY,
                new EffectConfig(EffectType.DAMAGE, 6)),
            card("defend", CardType.SKILL, TargetType.SELF, new EffectConfig(EffectType.BLOCK, 5)),
            card(
                "bandage", CardType.SKILL, TargetType.SELF, new EffectConfig(EffectType.HEAL, 4))));
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
  void shouldResolveAttackCardPlayDuringPlayerTurn() {
    advanceToPlayerTurn();
    List<String> played = new ArrayList<>();
    entity.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));

    entity.getEvents().trigger("playCard", "strike-instance", enemyId);

    // Card was accepted and, with no resolution service wired, resolved straight away.
    assertEquals(List.of("strike"), played);
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldStepThroughPlayerAttackPhaseWhenCardPlayed() {
    List<BattlePhase> phases = new ArrayList<>();
    entity.getEvents().addListener("phaseChange", (BattlePhase phase) -> phases.add(phase));
    advanceToPlayerTurn();

    entity.getEvents().trigger("playCard", "strike-instance", enemyId);

    assertTrue(phases.contains(BattlePhase.PLAYER_ATTACK));
  }

  @Test
  void shouldSubmitAttackCardWithAttackIntent() {
    BattleController mockController = mockControllerWithCards();
    GdxGame mockGame = mock(GdxGame.class);
    Entity battleUI = new Entity().addComponent(new BattleActions(mockController, mockGame));
    battleUI.create();

    battleUI.getEvents().trigger("playCard", "strike-instance", enemyId);

    verify(mockController)
        .submitCardPlayRequest(
            CardPlayRequest.singleEnemy("strike-instance", enemyId), PlayerIntent.ATTACK);
  }

  @Test
  void shouldSubmitBlockCardWithDefendIntent() {
    BattleController mockController = mockControllerWithCards();
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();

    battleUI.getEvents().trigger("playCard", "defend-instance", "player");

    verify(mockController)
        .submitCardPlayRequest(CardPlayRequest.self("defend-instance"), PlayerIntent.DEFEND);
  }

  @Test
  void shouldSubmitNonAttackCardWithOtherIntent() {
    BattleController mockController = mockControllerWithCards();
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();

    battleUI.getEvents().trigger("playCard", "bandage-instance", "player");

    verify(mockController)
        .submitCardPlayRequest(CardPlayRequest.self("bandage-instance"), PlayerIntent.OTHER);
  }

  @Test
  void shouldNotSubmitUnknownCard() {
    BattleController mockController = mockControllerWithCards();
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();

    battleUI.getEvents().trigger("playCard", "missing", "bone_crawler");

    verify(mockController, never()).submitCardPlayRequest(any(), any());
  }

  @Test
  void shouldNotFireCardPlayedWhenControllerRejectsRequest() {
    BattleController mockController = mockControllerWithCards();
    when(mockController.submitCardPlayRequest(any(), any())).thenReturn(false);
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mock(GdxGame.class)));
    battleUI.create();
    List<String> playedEvents = new ArrayList<>();
    battleUI
        .getEvents()
        .addListener(
            "cardPlayed",
            (String cardName, String targetId) -> playedEvents.add(cardName + ":" + targetId));

    battleUI.getEvents().trigger("playCard", "strike-instance", enemyId);

    assertTrue(playedEvents.isEmpty());
  }

  @Test
  void shouldEndTurnWhenProductionEndTurnEventFires() {
    BattleController mockController = mockControllerWithCards();
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
    BattleController realController =
        new BattleController(
            testPlayer, List.of(enemy), new CardEffectResolver(library), library, deck);
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

    battleUI
        .getEvents()
        .trigger("playCard", deck.getHand().get(0).instanceId(), Integer.toString(enemy.getId()));

    assertTrue(playedEvents.isEmpty());
    assertEquals(3, testPlayer.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(
        List.of("strike"),
        deck.getHand().stream().map(com.csse3200.game.cards.runtime.CardInstance::cardId).toList());
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
    entity.getEvents().trigger("playCard", "strike-instance", enemyId);

    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  private void advanceToPlayerTurn() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
  }

  @Test
  void shouldRejectCardWhileSilencedAndExplainWhy() {
    advanceToPlayerTurn();

    player.getComponent(CombatStatsComponent.class).applyStatusEffect("SILENCE", 1, 2);

    List<String> played = new ArrayList<>();
    List<String> logs = new ArrayList<>();

    entity.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));

    entity
        .getEvents()
        .addListener(BattleActions.BATTLE_LOG_EVENT, (String message) -> logs.add(message));

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertTrue(played.isEmpty());
    assertEquals(List.of("You are silenced and cannot play cards."), logs);
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldAllowCardAfterSilenceIsRemoved() {
    advanceToPlayerTurn();

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.applyStatusEffect("SILENCE", 1, 2);

    List<String> played = new ArrayList<>();
    entity.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");
    assertTrue(played.isEmpty());

    stats.removeStatusEffect("SILENCE");

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");
    assertEquals(List.of("strike"), played);
  }

  @Test
  void shouldExpireSilenceAfterTwoPlayerTurns() {
    advanceToPlayerTurn();

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.applyStatusEffect("SILENCE", 1, 2);

    List<String> played = new ArrayList<>();
    entity.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));

    // 被拒绝的出牌不能消耗状态的持续回合数。
    entity.getEvents().trigger("playCard", "strike", "bone_crawler");
    assertTrue(played.isEmpty());
    assertEquals(2, stats.getStatusEffect("SILENCE").getDuration());

    // 第一次结束回合：还剩一个回合，仍然不能出牌。
    entity.getEvents().trigger("endTurnSelected");
    assertTrue(stats.hasStatusEffect("SILENCE"));
    assertEquals(1, stats.getStatusEffect("SILENCE").getDuration());

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");
    assertTrue(played.isEmpty());
    assertEquals(1, stats.getStatusEffect("SILENCE").getDuration());

    // 第二次结束回合：状态自动移除。
    entity.getEvents().trigger("endTurnSelected");
    assertFalse(stats.hasStatusEffect("SILENCE"));

    // 自动解除后，可以正常出牌。
    entity.getEvents().trigger("playCard", "strike", "bone_crawler");
    assertEquals(List.of("strike"), played);
  }

  @Test
  void shouldDamagePlayerOnlyOnceAfterSuccessfulPlay() {
    advanceToPlayerTurn();

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertEquals(17, stats.getHealth());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldLoseBattleWhenCardPlayDamageKillsPlayer() {
    advanceToPlayerTurn();

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(2);
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertEquals(0, stats.getHealth());
    assertEquals(BattlePhase.DEFEAT, controller.getCurrentPhase());
  }

  // Holds the objects used by the three integration tests.
  private record DamageBattle(
      CombatStatsComponent stats,
      CombatStatsComponent enemyStats,
      EnergyComponent energy,
      BattleDeck deck,
      BattleController controller,
      Entity ui,
      List<String> played,
      List<Boolean> outcomes) {}

  // Creates a battle where each successful card play causes 3 damage.
  private DamageBattle createDamageBattle(int playerHealth, int enemyHealth, int cardCost) {

    CardConfig strike =
        card(
            "strike",
            CardType.ATTACK,
            TargetType.SINGLE_ENEMY,
            new EffectConfig(EffectType.DAMAGE, 6));
    strike.cost = cardCost;

    CardLibrary library = new CardLibrary(List.of(strike));
    BattleDeck deck = new BattleDeck(new PlayerDeck(library, List.of("strike")));
    deck.drawCards(1);

    CombatStatsComponent stats = new CombatStatsComponent(playerHealth, 0);
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);
    EnergyComponent energy = new EnergyComponent(3);

    Entity testPlayer = new Entity().addComponent(stats).addComponent(energy);

    CombatStatsComponent enemyStats = new CombatStatsComponent(enemyHealth, 1);
    Entity enemy =
        new Entity().addComponent(enemyStats).addComponent(new EnemyBehaviourComponent("test"));

    BattleController testController =
        new BattleController(
            testPlayer, List.of(enemy), new CardEffectResolver(library), library, deck);

    Entity ui =
        new Entity()
            .addComponent(new BattleActions(testController, mock(GdxGame.class), library))
            .addComponent(new DamageOnCardPlayComponent(stats));
    ui.create();

    List<String> played = new ArrayList<>();
    List<Boolean> outcomes = new ArrayList<>();

    ui.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));
    testController.addBattleEndListener(outcomes::add);

    testController.start();

    return new DamageBattle(stats, enemyStats, energy, deck, testController, ui, played, outcomes);
  }

  @Test
  void shouldNotTakeDamageWhenSilenceRejectsCard() {
    DamageBattle battle = createDamageBattle(20, 20, 1);
    battle.stats().applyStatusEffect("SILENCE", 1, 2);

    battle.ui().getEvents().trigger("playCard", "strike", "enemy");

    assertEquals(20, battle.stats().getHealth());
    assertEquals(20, battle.enemyStats().getHealth());
    assertEquals(3, battle.energy().getCurrentEnergy());
    assertEquals(List.of("strike"), battle.deck().getHand());
    assertTrue(battle.played().isEmpty());
    assertTrue(battle.outcomes().isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, battle.controller().getCurrentPhase());
  }

  @Test
  void shouldNotTakeDamageWhenEnergyIsInsufficient() {
    // The card costs 4 energy, but the player only has 3.
    DamageBattle battle = createDamageBattle(20, 20, 4);

    battle.ui().getEvents().trigger("playCard", "strike", "enemy");

    assertEquals(20, battle.stats().getHealth());
    assertEquals(20, battle.enemyStats().getHealth());
    assertEquals(3, battle.energy().getCurrentEnergy());
    assertEquals(List.of("strike"), battle.deck().getHand());
    assertTrue(battle.played().isEmpty());
    assertTrue(battle.outcomes().isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, battle.controller().getCurrentPhase());
  }

  @Test
  void shouldResolveDefeatOnceWhenBothSidesDieFromOnePlay() {
    // Strike deals 6 damage to an enemy with 5 HP.
    // The player has 2 HP and takes 3 damage after playing.
    DamageBattle battle = createDamageBattle(2, 5, 1);

    battle.ui().getEvents().trigger("playCard", "strike", "enemy");

    assertEquals(0, battle.enemyStats().getHealth());
    assertEquals(0, battle.stats().getHealth());
    assertEquals(2, battle.energy().getCurrentEnergy());
    assertEquals(List.of("strike"), battle.played());

    assertEquals(BattlePhase.DEFEAT, battle.controller().getCurrentPhase());

    // Exactly one result: false means defeat, true would mean victory.
    assertEquals(List.of(false), battle.outcomes());
  }
}
