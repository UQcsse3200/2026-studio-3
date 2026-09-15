package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.battle.BattleActions;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Team3CardPlayAdapterTest {

  @Test
  void shouldSubmitCardPlayRequestToBattleController() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));

    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    deck.drawOne();

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

    CardEffectHandler effectHandler = new CardEffectHandler();
    BattleController controller = new BattleController(player, enemies, effectHandler, playService);

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(cards, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    // Advance the controller into the player's turn.
    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    // The card was resolved through the controller/service.
    assertEquals(2, energy.getCurrentEnergy());

    // strike was discarded and not replaced.
    assertEquals(List.of(), deck.getHand());
    assertEquals(List.of("strike"), deck.getDiscardPile());

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
    card.description = "Add armor";
    card.cost = 1;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SELF;
    card.texturePath = "images/cards/defend.png";
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 3)};
    return card;
  }

  @Test
  void shouldRejectCardWhenPlayerIsSilenced() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    deck.drawOne();

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

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(cards, controller);

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

    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertEquals(List.of("You are silenced and cannot play cards."), logs);

    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of("strike"), deck.getHand());
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

    deck.drawOne();

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

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(cards, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    List<String> played = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String cardId, String targetId) -> played.add(cardId));

    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertEquals(List.of("strike"), played);
    assertEquals(2, energy.getCurrentEnergy());
    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldAllowCardAfterSilenceIsRemoved() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    deck.drawOne();

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

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(cards, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    playerStats.applyStatusEffect("SILENCE", 1, 2);

    List<String> played = new ArrayList<>();

    battleFlow
        .getEvents()
        .addListener(
            Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
            (String cardId, String targetId) -> played.add(cardId));

    // First attempt is rejected.
    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertTrue(played.isEmpty());
    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of("strike"), deck.getHand());

    playerStats.removeStatusEffect("SILENCE");

    // Second attempt is allowed.
    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertEquals(List.of("strike"), played);
    assertEquals(2, energy.getCurrentEnergy());
    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotLogSilenceMessageWhenPlayerIsNotSilenced() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));
    BattleDeck deck = new BattleDeck(new PlayerDeck(cards, List.of("strike", "defend")));

    deck.drawOne();

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

    Team3CardPlayAdapter adapter = new Team3CardPlayAdapter(cards, controller);

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

    battleFlow.getEvents().trigger(Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertFalse(logs.contains("You are silenced and cannot play cards."));
  }
}
