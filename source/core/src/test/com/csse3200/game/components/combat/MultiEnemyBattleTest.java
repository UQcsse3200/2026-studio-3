package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.*;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises card resolution and complete rounds against independently targetable enemies. */
class MultiEnemyBattleTest {
  private final Entity player =
      new Entity()
          .addComponent(new CombatStatsComponent(100, 0))
          .addComponent(new EnergyComponent(3));
  private final Entity first = enemy(30, 5);
  private final Entity second = enemy(24, 6);
  private BattleDeck deck;

  private static Entity enemy(int health, int attack) {
    // Even two enemies with the same display name must be independently targetable.
    return new Entity()
        .addComponent(new CombatStatsComponent(health, attack))
        .addComponent(new EnemyStatsComponent("Lesser Shade"))
        .addComponent(new EnemyBehaviourComponent("cycle_attack_defend"));
  }

  private BattleController battle(TargetType target, EffectType effect, int value) {
    return battle(target, effect, value, Map.of());
  }

  private BattleController battle(
      TargetType target, EffectType effect, int value, Map<String, Entity> targets) {
    CardConfig card = new CardConfig();
    card.id = "test_card";
    card.name = "Test card";
    card.description = "Test card";
    card.texturePath = "test.png";
    card.type = effect == EffectType.DAMAGE ? CardType.ATTACK : CardType.SKILL;
    card.target = target;
    card.cost = 1;
    card.effects = new EffectConfig[] {new EffectConfig(effect, value)};
    CardLibrary library = new CardLibrary(List.of(card));
    deck = new BattleDeck(new PlayerDeck(library, List.of(card.id, card.id, card.id)));
    deck.drawCards(1);
    CardEffectHandler effects = new CardEffectHandler(targets);
    BattleController controller =
        new BattleController(
            player,
            List.of(first, second),
            effects,
            new CardPlayService(library, deck, player.getComponent(EnergyComponent.class)));
    controller.start();
    return controller;
  }

  private boolean strike(BattleController controller, Entity target) {
    return controller.submitCardPlayRequest(
        CardPlayRequest.singleEnemy("test_card", Integer.toString(target.getId())));
  }

  @Test
  void existingFactoryTargetIdsSelectOnlyTheirRegisteredEnemy() {
    BattleController controller =
        battle(
            TargetType.SINGLE_ENEMY,
            EffectType.DAMAGE,
            6,
            Map.of("bone_crawler", first, "lesser_shade_1", second));
    // Each attack needs its own dealt copy while the played card is on cooldown.
    deck.drawCards(1);
    assertTrue(
        controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy("test_card", "lesser_shade_1")));
    assertEquals(30, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(18, second.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(
        controller.submitCardPlayRequest(CardPlayRequest.singleEnemy("test_card", "bone_crawler")));
    assertEquals(24, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(18, second.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void registryRejectsUnregisteredOrDefeatedTargetsWithoutSpendingEnergy() {
    BattleController controller =
        battle(
            TargetType.SINGLE_ENEMY,
            EffectType.DAMAGE,
            6,
            Map.of("bone_crawler", first, "lesser_shade_1", second));
    assertFalse(strike(controller, second)); // An entity ID is not a registered drop-target ID.
    second.getComponent(CombatStatsComponent.class).setHealth(0);
    assertFalse(
        controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy("test_card", "lesser_shade_1")));
    assertEquals(3, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(List.of("test_card"), deck.getHand());
    assertTrue(deck.getDiscardPile().isEmpty());
  }

  @Test
  void singleTargetCardOnlyDamagesTheSelectedEnemyAndSpendsOnce() {
    BattleController controller = battle(TargetType.SINGLE_ENEMY, EffectType.DAMAGE, 6);
    assertTrue(strike(controller, second));
    assertEquals(30, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(18, second.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(2, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(1, deck.getDiscardPile().size());
    assertTrue(deck.getHand().isEmpty());
  }

  @Test
  void unknownTargetDoesNotSpendEnergyOrMoveTheCard() {
    BattleController controller = battle(TargetType.SINGLE_ENEMY, EffectType.DAMAGE, 6);
    assertFalse(
        controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy("test_card", "missing-enemy")));
    assertEquals(3, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(List.of("test_card"), deck.getHand());
    assertTrue(deck.getDiscardPile().isEmpty());
    assertEquals(30, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(24, second.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void deadTargetDoesNotConsumeCardOrDamageSurvivor() {
    BattleController controller = battle(TargetType.SINGLE_ENEMY, EffectType.DAMAGE, 6);
    first.getComponent(CombatStatsComponent.class).setHealth(0);
    assertFalse(strike(controller, first));
    assertEquals(3, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertTrue(deck.getDiscardPile().isEmpty());
    assertEquals(24, second.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void killingFirstEnemySkipsItsTurnAndVictoryWaitsForSecondEnemy() {
    BattleController controller = battle(TargetType.SINGLE_ENEMY, EffectType.DAMAGE, 30);
    // Keep a second copy in hand to attack the survivor before the first card's cooldown ends.
    deck.drawCards(1);
    List<Boolean> outcomes = new ArrayList<>();
    controller.addBattleEndListener(outcomes::add);
    assertTrue(strike(controller, first));
    assertTrue(outcomes.isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    controller.endPlayerTurn();
    assertEquals(94, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(strike(controller, second));
    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
    assertEquals(List.of(true), outcomes);
  }

  @Test
  void everyLivingEnemyActsOncePerRound() {
    BattleController controller = battle(TargetType.SINGLE_ENEMY, EffectType.DAMAGE, 6);
    controller.endPlayerTurn();
    assertEquals(89, player.getComponent(CombatStatsComponent.class).getHealth());
    controller.endPlayerTurn(); // Both defend.
    assertEquals(89, player.getComponent(CombatStatsComponent.class).getHealth());
    controller.endPlayerTurn();
    assertEquals(78, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void allEnemyDamageHitsBothEnemiesAndCanWinTheBattle() {
    BattleController controller = battle(TargetType.ALL_ENEMIES, EffectType.DAMAGE, 30);
    assertTrue(controller.submitCardPlayRequest(CardPlayRequest.allEnemies("test_card")));
    assertTrue(first.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(second.getComponent(CombatStatsComponent.class).isDead());
    assertEquals(BattlePhase.VICTORY, controller.getCurrentPhase());
    assertEquals(2, player.getComponent(EnergyComponent.class).getCurrentEnergy());
  }

  @Test
  void selfCardProtectsPlayerFromTheCombinedEnemyAttacks() {
    BattleController controller = battle(TargetType.SELF, EffectType.BLOCK, 8);
    assertTrue(controller.submitCardPlayRequest(CardPlayRequest.self("test_card")));
    assertEquals(30, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(24, second.getComponent(CombatStatsComponent.class).getHealth());
    controller.endPlayerTurn();
    assertEquals(97, player.getComponent(CombatStatsComponent.class).getHealth());
  }
}
