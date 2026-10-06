package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.battle.BattleActions;
import com.csse3200.game.components.battle.DamageOnCardPlayComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Team3CardPlayAdapterTest {

  @Test
  void shouldRejectOtherEnemyDuringTauntWithoutSpendingEnergyOrPlayingTheCard() {
    CardLibrary cards = new CardLibrary(List.of(strike()));
    CardInstance strikeInstance = new CardInstance("strike-taunt", "strike", 0);
    BattleDeck deck = new BattleDeck(PlayerDeck.fromInstances(cards, List.of(strikeInstance)));
    deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(20, 1);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);
    Entity taunter =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    Entity other =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    String taunterId = Integer.toString(taunter.getId());
    String otherId = Integer.toString(other.getId());
    Map<String, Entity> targets = Map.of(taunterId, taunter, otherId, other);
    CardPlayService playService =
        new CardPlayService(
            cards,
            deck,
            energy,
            new Team7PlayerStateAdapter(energy, playerStats),
            new Team1EnemyStateAdapter(targets));
    BattleController controller =
        new BattleController(
            player, List.of(taunter, other), new CardEffectHandler(targets), playService);
    Entity battleFlow =
        new Entity().addComponent(new Team3CardPlayAdapter(playService, controller));
    battleFlow.create();
    controller.start();
    playerStats.applyStatusEffect("TAUNT:" + taunterId, taunter.getId(), 2);

    List<String> logs = new ArrayList<>();
    List<String> played = new ArrayList<>();
    battleFlow
        .getEvents()
        .addListener(BattleActions.BATTLE_LOG_EVENT, (EventListener1<String>) logs::add);
    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> played.add(instanceId));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), otherId);

    assertEquals(List.of("A taunting enemy must be targeted."), logs);
    assertTrue(played.isEmpty());
    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of(strikeInstance), deck.getHand());
    assertEquals(10, other.getComponent(CombatStatsComponent.class).getHealth());

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), taunterId);

    assertEquals(List.of(strikeInstance.instanceId()), played);
    assertEquals(2, energy.getCurrentEnergy());
    assertEquals(List.of(), deck.getHand());
    assertEquals(4, taunter.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldAllowEitherTaunterAndOtherEnemiesAfterBothTauntersDie() {
    CardLibrary cards = new CardLibrary(List.of(strike()));
    CardInstance firstStrike = new CardInstance("strike-first-taunt", "strike", 0);
    CardInstance secondStrike = new CardInstance("strike-second-taunt", "strike", 0);
    CardInstance thirdStrike = new CardInstance("strike-after-taunt", "strike", 0);
    BattleDeck deck =
        new BattleDeck(
            PlayerDeck.fromInstances(cards, List.of(firstStrike, secondStrike, thirdStrike)));
    deck.drawCards(3);

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(20, 1);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);
    Entity firstTaunter =
        new Entity()
            .addComponent(new CombatStatsComponent(6, 1))
            .addComponent(new EnemyStatsComponent("First taunter"))
            .addComponent(new EnemyBehaviourComponent("test"));
    Entity secondTaunter =
        new Entity()
            .addComponent(new CombatStatsComponent(6, 1))
            .addComponent(new EnemyStatsComponent("Second taunter"))
            .addComponent(new EnemyBehaviourComponent("test"));
    Entity other =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    firstTaunter.create();
    secondTaunter.create();

    String firstId = Integer.toString(firstTaunter.getId());
    String secondId = Integer.toString(secondTaunter.getId());
    String otherId = Integer.toString(other.getId());
    Map<String, Entity> targets =
        Map.of(firstId, firstTaunter, secondId, secondTaunter, otherId, other);
    CardPlayService playService =
        new CardPlayService(
            cards,
            deck,
            energy,
            new Team7PlayerStateAdapter(energy, playerStats),
            new Team1EnemyStateAdapter(targets));
    BattleController controller =
        new BattleController(
            player,
            List.of(firstTaunter, secondTaunter, other),
            new CardEffectHandler(targets),
            playService);
    Entity battleFlow =
        new Entity().addComponent(new Team3CardPlayAdapter(playService, controller));
    battleFlow.create();
    controller.start();
    playerStats.applyStatusEffect("TAUNT:" + firstId, firstTaunter.getId(), 2);
    playerStats.applyStatusEffect("TAUNT:" + secondId, secondTaunter.getId(), 2);
    assertEquals(List.of(firstId, secondId), controller.getAliveTaunterTargetIds());

    List<String> played = new ArrayList<>();
    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> played.add(instanceId));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, firstStrike.instanceId(), secondId);
    assertFalse(playerStats.hasStatusEffect("TAUNT:" + secondId));
    assertEquals(List.of(firstId), controller.getAliveTaunterTargetIds());

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, secondStrike.instanceId(), firstId);
    assertFalse(playerStats.hasStatusEffect("TAUNT:" + firstId));
    assertEquals(List.of(), controller.getAliveTaunterTargetIds());

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, thirdStrike.instanceId(), otherId);

    assertEquals(
        List.of(firstStrike.instanceId(), secondStrike.instanceId(), thirdStrike.instanceId()),
        played);
    assertEquals(0, energy.getCurrentEnergy());
    assertEquals(List.of(), deck.getHand());
    assertEquals(4, other.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldAllowSelfAndAllEnemyCardsDuringTaunt() {
    CardConfig self = defend();
    CardConfig allEnemies = strike();
    allEnemies.id = "sweep";
    allEnemies.name = "Sweep";
    allEnemies.target = TargetType.ALL_ENEMIES;
    CardLibrary cards = new CardLibrary(List.of(self, allEnemies));
    CardInstance selfInstance = new CardInstance("defend-taunt", "defend", 0);
    CardInstance allInstance = new CardInstance("sweep-taunt", "sweep", 0);
    BattleDeck deck =
        new BattleDeck(PlayerDeck.fromInstances(cards, List.of(selfInstance, allInstance)));
    deck.drawCards(2);

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(20, 1);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);
    Entity taunter =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    Entity other =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    String taunterId = Integer.toString(taunter.getId());
    String otherId = Integer.toString(other.getId());
    Map<String, Entity> targets = Map.of(taunterId, taunter, otherId, other);
    CardPlayService playService =
        new CardPlayService(
            cards,
            deck,
            energy,
            new Team7PlayerStateAdapter(energy, playerStats),
            new Team1EnemyStateAdapter(targets));
    BattleController controller =
        new BattleController(
            player, List.of(taunter, other), new CardEffectHandler(targets), playService);
    Entity battleFlow =
        new Entity().addComponent(new Team3CardPlayAdapter(playService, controller));
    battleFlow.create();
    controller.start();
    playerStats.applyStatusEffect("TAUNT:" + taunterId, taunter.getId(), 2);

    List<String> played = new ArrayList<>();
    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> played.add(instanceId));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, selfInstance.instanceId(), "player");
    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, allInstance.instanceId(), otherId);

    assertEquals(List.of(selfInstance.instanceId(), allInstance.instanceId()), played);
    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(List.of(), deck.getHand());
    assertEquals(4, taunter.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(4, other.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldPlayOnlyTheSelectedUpgradedDuplicateByInstanceId() {
    CardConfig strike = upgradedStrike();
    CardLibrary cards = new CardLibrary(List.of(strike));
    CardInstance baseStrike = new CardInstance("strike-base", "strike", 0);
    CardInstance upgradedStrike = new CardInstance("strike-plus", "strike", 1);
    BattleDeck deck =
        new BattleDeck(PlayerDeck.fromInstances(cards, List.of(baseStrike, upgradedStrike)));
    deck.drawCards(2);

    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 1)).addComponent(energy);
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    Team7PlayerStateAdapter playerState =
        new Team7PlayerStateAdapter(energy, player.getComponent(CombatStatsComponent.class));
    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));
    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectHandler(Map.of("enemy-1", enemy)), playService);
    Entity battleFlow =
        new Entity().addComponent(new Team3CardPlayAdapter(playService, controller));
    battleFlow.create();
    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, upgradedStrike.instanceId(), "enemy-1");

    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(3, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(List.of(baseStrike), deck.getHand());
    assertEquals(List.of(upgradedStrike), deck.getDiscardPile());
  }

  @Test
  void shouldSubmitCardPlayRequestToBattleController() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));

    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);

    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 1)).addComponent(energy);

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    List<Entity> enemies = List.of(enemy);

    Team7PlayerStateAdapter playerState =
        new Team7PlayerStateAdapter(energy, player.getComponent(CombatStatsComponent.class));

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    CardEffectHandler effectHandler = new CardEffectHandler(Map.of("enemy-1", enemy));
    BattleController controller = new BattleController(player, enemies, effectHandler, playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(playService, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    // Advance the controller into the player's turn.
    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    // The card was resolved through the controller/service.
    assertEquals(2, energy.getCurrentEnergy());

    // strike was discarded and not replaced.
    assertEquals(List.of(), deck.getHand());
    assertEquals(List.of(strikeInstance), deck.getDiscardPile());

    // The BattleController applies the resolved card effects.
    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  private static CardConfig strike() {
    CardConfig card = new CardConfig();
    card.id = "strike";
    card.name = "Strike";
    card.description = "Deal damage";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.texturePath = "images/cards/strike.png";
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    return card;
  }

  private static CardConfig defend() {
    CardConfig card = new CardConfig();
    card.id = "defend";
    card.name = "Defend";
    card.description = "Add armour";
    card.cost = 1;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SELF;
    card.texturePath = "images/cards/defend.png";
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 3)};
    return card;
  }

  private static CardConfig upgradedStrike() {
    CardConfig card = strike();
    CardUpgradeConfig upgrade = new CardUpgradeConfig();
    upgrade.name = "Strike+";
    upgrade.description = "Deal 9 damage";
    upgrade.cost = 2;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 9)};
    card.upgrade = upgrade;
    return card;
  }

  @Test
  void shouldRejectCardWhenPlayerIsSilenced() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);

    CombatStatsComponent playerStats = new CombatStatsComponent(10, 1);

    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    List<Entity> enemies = List.of(enemy);

    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy, playerStats);

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    CardEffectHandler effectHandler = new CardEffectHandler();

    BattleController controller = new BattleController(player, enemies, effectHandler, playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(playService, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    playerStats.applyStatusEffect("SILENCE", 1, 2);

    List<String> logs = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(BattleActions.BATTLE_LOG_EVENT, (String message) -> logs.add(message));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertEquals(List.of("You are silenced and cannot play cards."), logs);

    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of(strikeInstance), deck.getHand());
    assertEquals(List.of(), deck.getDiscardPile());
    assertEquals(10, enemy.getComponent(CombatStatsComponent.class).getHealth());

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldAllowCardWhenPlayerIsNotSilenced() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(10, 1);

    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy, playerStats);

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    CardEffectHandler effectHandler = new CardEffectHandler(Map.of("enemy-1", enemy));

    BattleController controller =
        new BattleController(player, List.of(enemy), effectHandler, playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(playService, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();
    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    List<String> playedInstances = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> playedInstances.add(instanceId));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertEquals(List.of(strikeInstance.instanceId()), playedInstances);

    assertEquals(2, energy.getCurrentEnergy());
    assertTrue(deck.getHand().isEmpty());
    assertEquals(List.of(strikeInstance), deck.getDiscardPile());

    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldAllowCardAfterSilenceIsRemoved() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(10, 1);

    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy, playerStats);

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    CardEffectHandler effectHandler = new CardEffectHandler(Map.of("enemy-1", enemy));

    BattleController controller =
        new BattleController(player, List.of(enemy), effectHandler, playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(playService, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();
    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    playerStats.applyStatusEffect("SILENCE", 1, 2);

    List<String> playedInstances = new ArrayList<>();
    List<String> logs = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> playedInstances.add(instanceId));

    battleFlow
        .getEvents()
        .addListener(BattleActions.BATTLE_LOG_EVENT, (EventListener1<String>) logs::add);
    // The first attempt must be rejected while silence is active.
    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertTrue(playedInstances.isEmpty());
    assertEquals(List.of("You are silenced and cannot play cards."), logs);

    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of(strikeInstance), deck.getHand());
    assertTrue(deck.getDiscardPile().isEmpty());

    assertEquals(10, enemy.getComponent(CombatStatsComponent.class).getHealth());

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    // Removing silence should allow the same card instance to be played.
    playerStats.removeStatusEffect("SILENCE");

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertEquals(List.of(strikeInstance.instanceId()), playedInstances);

    assertEquals(2, energy.getCurrentEnergy());
    assertTrue(deck.getHand().isEmpty());
    assertEquals(List.of(strikeInstance), deck.getDiscardPile());

    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldNotLogSilenceMessageWhenPlayerIsNotSilenced() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);

    CombatStatsComponent playerStats = new CombatStatsComponent(10, 1);

    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy, playerStats);

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    BattleController controller =
        new BattleController(player, List.of(enemy), new CardEffectHandler(), playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(playService, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    List<String> logs = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(BattleActions.BATTLE_LOG_EVENT, (String message) -> logs.add(message));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");

    assertFalse(logs.contains("You are silenced and cannot play cards."));
  }

  @Test
  void shouldExpireSilenceAfterTwoPlayerTurns() {
    CardConfig strike = strike();
    CardLibrary cards = new CardLibrary(List.of(strike));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike")));
    CardInstance strikeInstance = deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(20, 1);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));

    CardPlayService playService =
        new CardPlayService(
            cards,
            deck,
            energy,
            new Team7PlayerStateAdapter(energy, playerStats),
            new Team1EnemyStateAdapter(Map.of("enemy-1", enemy)));
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectHandler(Map.of("enemy-1", enemy)), playService);
    Entity battleFlow =
        new Entity().addComponent(new Team3CardPlayAdapter(playService, controller));
    battleFlow.create();
    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    playerStats.applyStatusEffect("SILENCE", 1, 2);
    List<String> playedInstances = new ArrayList<>();
    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> playedInstances.add(instanceId));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");
    assertTrue(playedInstances.isEmpty());
    assertEquals(2, playerStats.getStatusEffect("SILENCE").getDuration());

    controller.endPlayerTurn();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(1, playerStats.getStatusEffect("SILENCE").getDuration());

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");
    assertTrue(playedInstances.isEmpty());
    assertEquals(1, playerStats.getStatusEffect("SILENCE").getDuration());

    controller.endPlayerTurn();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertFalse(playerStats.hasStatusEffect("SILENCE"));

    battleFlow
        .getEvents()
        .trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, strikeInstance.instanceId(), "enemy-1");
    assertEquals(List.of(strikeInstance.instanceId()), playedInstances);
    assertEquals(2, energy.getCurrentEnergy());
  }

  private record DamageBattle(
      CombatStatsComponent playerStats,
      CombatStatsComponent enemyStats,
      EnergyComponent energy,
      BattleDeck deck,
      BattleController controller,
      Entity battleFlow,
      CardInstance strikeInstance,
      List<String> playedInstances,
      List<Boolean> outcomes) {}

  private DamageBattle createDamageBattle(int playerHealth, int enemyHealth, int cardCost) {
    CardConfig strike = strike();
    strike.cost = cardCost;

    CardLibrary cards = new CardLibrary(List.of(strike));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike")));
    CardInstance strikeInstance = deck.drawOne();

    CombatStatsComponent playerStats = new CombatStatsComponent(playerHealth, 0);
    playerStats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    CombatStatsComponent enemyStats = new CombatStatsComponent(enemyHealth, 1);
    Entity enemy =
        new Entity().addComponent(enemyStats).addComponent(new EnemyBehaviourComponent("test"));

    CardPlayService playService =
        new CardPlayService(
            cards,
            deck,
            energy,
            new Team7PlayerStateAdapter(energy, playerStats),
            new Team1EnemyStateAdapter(Map.of("enemy", enemy)));
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectHandler(Map.of("enemy", enemy)), playService);
    Entity battleFlow =
        new Entity()
            .addComponent(new Team3CardPlayAdapter(playService, controller))
            .addComponent(new DamageOnCardPlayComponent(playerStats));
    battleFlow.create();

    List<String> playedInstances = new ArrayList<>();
    List<Boolean> outcomes = new ArrayList<>();
    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String instanceId, String targetId) -> playedInstances.add(instanceId));
    controller.addBattleEndListener(outcomes::add);
    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    return new DamageBattle(
        playerStats,
        enemyStats,
        energy,
        deck,
        controller,
        battleFlow,
        strikeInstance,
        playedInstances,
        outcomes);
  }

  @Test
  void shouldDamagePlayerOnlyOnceAfterSuccessfulPlay() {
    DamageBattle battle = createDamageBattle(20, 20, 1);

    battle
        .battleFlow()
        .getEvents()
        .trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, battle.strikeInstance().instanceId(), "enemy");

    assertEquals(17, battle.playerStats().getHealth());
    assertEquals(List.of(battle.strikeInstance().instanceId()), battle.playedInstances());
    assertEquals(BattlePhase.PLAYER_TURN, battle.controller().getCurrentPhase());
  }

  @Test
  void shouldLoseBattleWhenCardPlayDamageKillsPlayer() {
    DamageBattle battle = createDamageBattle(2, 20, 1);

    battle
        .battleFlow()
        .getEvents()
        .trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, battle.strikeInstance().instanceId(), "enemy");

    assertEquals(0, battle.playerStats().getHealth());
    assertEquals(BattlePhase.DEFEAT, battle.controller().getCurrentPhase());
    assertEquals(List.of(false), battle.outcomes());
  }

  @Test
  void shouldNotTakeDamageWhenSilenceRejectsCard() {
    DamageBattle battle = createDamageBattle(20, 20, 1);
    battle.playerStats().applyStatusEffect("SILENCE", 1, 2);

    battle
        .battleFlow()
        .getEvents()
        .trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, battle.strikeInstance().instanceId(), "enemy");

    assertEquals(20, battle.playerStats().getHealth());
    assertEquals(20, battle.enemyStats().getHealth());
    assertEquals(3, battle.energy().getCurrentEnergy());
    assertEquals(List.of(battle.strikeInstance()), battle.deck().getHand());
    assertTrue(battle.deck().getDiscardPile().isEmpty());
    assertTrue(battle.playedInstances().isEmpty());
    assertTrue(battle.outcomes().isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, battle.controller().getCurrentPhase());
  }

  @Test
  void shouldNotTakeDamageWhenEnergyIsInsufficient() {
    DamageBattle battle = createDamageBattle(20, 20, 4);

    battle
        .battleFlow()
        .getEvents()
        .trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, battle.strikeInstance().instanceId(), "enemy");

    assertEquals(20, battle.playerStats().getHealth());
    assertEquals(20, battle.enemyStats().getHealth());
    assertEquals(3, battle.energy().getCurrentEnergy());
    assertEquals(List.of(battle.strikeInstance()), battle.deck().getHand());
    assertTrue(battle.deck().getDiscardPile().isEmpty());
    assertTrue(battle.playedInstances().isEmpty());
    assertTrue(battle.outcomes().isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, battle.controller().getCurrentPhase());
  }

  @Test
  void shouldResolveDefeatOnceWhenBothSidesDieFromOnePlay() {
    DamageBattle battle = createDamageBattle(2, 5, 1);

    battle
        .battleFlow()
        .getEvents()
        .trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, battle.strikeInstance().instanceId(), "enemy");

    assertEquals(0, battle.enemyStats().getHealth());
    assertEquals(0, battle.playerStats().getHealth());
    assertEquals(2, battle.energy().getCurrentEnergy());
    assertEquals(List.of(battle.strikeInstance().instanceId()), battle.playedInstances());
    assertEquals(BattlePhase.DEFEAT, battle.controller().getCurrentPhase());
    assertEquals(List.of(false), battle.outcomes());
  }
}
