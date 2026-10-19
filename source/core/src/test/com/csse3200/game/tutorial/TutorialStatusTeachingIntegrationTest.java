package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.integration.Team1EnemyStateAdapter;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyAI.TutorialEnemyAI;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TutorialStatusTeachingIntegrationTest {
  @Test
  void realPoisonPlayTeachesStacksDurationAndExpiryWithoutConsumingThePersistentDeck() {
    var library = new CardLibrary(CardConfigLoader.loadCards());
    var playerDeck = new PlayerDeck(library, List.of("strike", "poison_dagger"));
    var originalCards = playerDeck.getCards();
    var deck = new BattleDeck(playerDeck);
    deck.drawCards(2);
    String strike = deck.getHand().get(0).instanceId();
    String poison = deck.getHand().get(1).instanceId();
    var energy = new EnergyComponent(3);
    var playerStats = new CombatStatsComponent(100, 0);
    var player = new Entity().addComponent(playerStats).addComponent(energy);
    var enemyStats = new CombatStatsComponent(32, 6);
    var behaviour = new EnemyBehaviourComponent("cycle_attack_defend");
    behaviour.setAI(new TutorialEnemyAI());
    var enemy = new Entity().addComponent(enemyStats).addComponent(behaviour);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.getEntityService().register(player);
    ServiceLocator.getEntityService().register(enemy);
    var targets = Map.of("enemy", enemy);
    var play =
        new CardPlayService(
            library,
            new CardEffectResolutionService(library),
            deck,
            energy,
            new Team7PlayerStateAdapter(player),
            new Team1EnemyStateAdapter(targets));
    var battle = new BattleController(player, List.of(enemy), new CardEffectHandler(targets), play);
    try (var tutorial = new BattleTutorialController(true)) {
      tutorial.setDemonstrationCardId(strike);
      tutorial.setPoisonCardId(poison);
      tutorial.observeInitialHealth(32, 100);
      tutorial.bindTo(battle);
      tutorial.start();
      battle.start();
      for (int i = 0; i < 5; i++) assertTrue(tutorial.continueInformation());
      assertTrue(battle.submitCardPlayRequest(CardPlayRequest.singleEnemy(strike, "enemy")));
      tutorial.observeResolution(
          enemyStats.getHealth(), playerStats.getHealth(), true, battle.getCurrentPhase(), false);
      assertEquals(
          BattleTutorialController.Step.ENEMY_STATS, tutorial.getCurrentStep().orElseThrow());
      assertTrue(tutorial.continueInformation());
      assertFalse(battle.submitCardPlayRequest(CardPlayRequest.singleEnemy(poison, "missing")));
      energy.setCurrentEnergy(0);
      assertFalse(battle.submitCardPlayRequest(CardPlayRequest.singleEnemy(poison, "enemy")));
      assertEquals(
          BattleTutorialController.Step.PLAY_POISON_CARD, tutorial.getCurrentStep().orElseThrow());
      energy.setCurrentEnergy(2);
      assertTrue(battle.submitCardPlayRequest(CardPlayRequest.singleEnemy(poison, "enemy")));
      assertEquals(1, energy.getCurrentEnergy());
      tutorial.observeResolution(
          enemyStats.getHealth(),
          playerStats.getHealth(),
          true,
          battle.getCurrentPhase(),
          enemyStats.hasStatusEffect("POISON"));
      assertEquals(
          BattleTutorialController.Step.STATUS_EFFECTS, tutorial.getCurrentStep().orElseThrow());
      assertEquals(22, enemyStats.getHealth());
      assertEquals(3, enemyStats.getStatusEffect("POISON").getValue());
      for (int remaining = 3; remaining > 0; remaining--) {
        assertEquals(remaining, enemyStats.getStatusEffect("POISON").getDuration());
        enemyStats.setBlock(10);
        enemyStats.setArmour(10);
        int before = enemyStats.getHealth();
        battle.endPlayerTurn();
        assertEquals(before - 3, enemyStats.getHealth());
        assertTrue(playerStats.getHealth() > 0);
      }
      assertFalse(enemyStats.hasStatusEffect("POISON"));
      assertEquals(originalCards, playerDeck.getCards());
    } finally {
      enemy.dispose();
      player.dispose();
    }
  }
}
