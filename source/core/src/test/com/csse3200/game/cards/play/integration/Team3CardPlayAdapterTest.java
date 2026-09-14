package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
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

    Entity player = new Entity()
                    .addComponent(new CombatStatsComponent(10, 1))
                    .addComponent(energy);

    Entity enemy = new Entity()
                    .addComponent(new CombatStatsComponent(10, 1))
                    .addComponent(new EnemyBehaviourComponent("test"));

    List<Entity> enemies = List.of(enemy);

    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy,
                    player.getComponent(CombatStatsComponent.class));

    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(Map.of("enemy-1", enemy));

    CardPlayService playService = new CardPlayService(cards, deck, energy, playerState, enemyState);

    CardEffectHandler effectHandler = new CardEffectHandler();
    BattleController controller = new BattleController(player, enemies, effectHandler, playService);

    Team3CardPlayAdapter adapter =
            new Team3CardPlayAdapter(cards, controller);

    Entity battleFlow = new Entity().addComponent(adapter);

    battleFlow.create();

    controller.start();

    // Advance the controller into the player's turn.
    while (controller.getCurrentPhase() != BattlePhase.PLAYER_TURN) {
      controller.endPlayerTurn();
    }

    battleFlow.getEvents().trigger(
            Team3CardPlayAdapter.PLAY_CARD_EVENT, "strike", "enemy-1");

    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    // The card was resolved through the controller/service.
    assertEquals(2, energy.getCurrentEnergy());

    // strike was discarded and defend was drawn as the replacement.
    assertEquals(List.of("defend"), deck.getHand());
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
}

