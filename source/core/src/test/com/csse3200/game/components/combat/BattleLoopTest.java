package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** End-to-end checks for the player-turn / enemy-turn / win-loss battle loop. */
class BattleLoopTest {

  private static CardConfig strikeCard() {
    CardConfig strike = new CardConfig();
    strike.id = "strike";
    strike.name = "Strike";
    strike.cost = 1;
    strike.type = CardType.ATTACK;
    strike.target = TargetType.SINGLE_ENEMY;
    strike.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    strike.texturePath = "images/cards/strike.png";
    return strike;
  }

  @Test
  void playerWinsWhenCardKillsTheLastEnemy() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(6, 4))
            .addComponent(new EnemyBehaviourComponent("test"));

    // Hand of 1 (strike) with a spare in the draw pile so the played card is replaced by a draw.
    BattleDeck deck = new BattleDeck(new PlayerDeck(List.of("strike", "bandage")));
    deck.drawCards(1);

    CardLibrary library = new CardLibrary(List.of(strikeCard()));
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectResolver(library), library, deck);
    AtomicReference<Boolean> outcome = new AtomicReference<>();
    List<String> log = new ArrayList<>();
    controller.addBattleEndListener(outcome::set);
    controller.addBattleLogListener(log::add);

    controller.start();
    controller.submitCardPlayRequest(new CardPlayRequest("strike", "enemy"), PlayerIntent.ATTACK);

    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
    assertEquals(Boolean.TRUE, outcome.get());
    assertEquals(0, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertFalse(deck.getHand().contains("strike"));
    assertTrue(deck.getDiscardPile().contains("strike"));
    assertEquals(List.of("bandage"), deck.getHand()); // played strike replaced by a draw
    assertTrue(log.stream().anyMatch(line -> line.contains("Victory")));
  }

  @Test
  void playerLosesWhenTheEnemyReducesHealthToZero() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(200, 25))
            .addComponent(new EnemyBehaviourComponent("test"));

    BattleController controller = new BattleController(player, List.of(enemy));
    AtomicReference<Boolean> outcome = new AtomicReference<>();
    controller.addBattleEndListener(outcome::set);

    controller.start();
    // Player ends their turn without acting; the enemy hits for 25 and drops the player.
    controller.endPlayerTurn();

    assertEquals(BattlePhase.DEFEAT, controller.getCurrentPhase());
    assertEquals(Boolean.FALSE, outcome.get());
    assertEquals(0, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void loopReturnsToPlayerTurnAfterEachRoundWhileBothSidesLive() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(200, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(200, 3))
            .addComponent(new EnemyBehaviourComponent("test"));

    BattleController controller = new BattleController(player, List.of(enemy));

    controller.start();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());

    // Round 1: the enemy AI opens by attacking, hitting the player for its base attack of 3.
    controller.endPlayerTurn();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(197, player.getComponent(CombatStatsComponent.class).getHealth());

    // Round 2: the AI cycles to DEFEND, so the player takes no damage but the loop still resumes.
    controller.endPlayerTurn();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(197, player.getComponent(CombatStatsComponent.class).getHealth());

    // Round 3: the AI attacks again.
    controller.endPlayerTurn();
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(194, player.getComponent(CombatStatsComponent.class).getHealth());
  }
}
