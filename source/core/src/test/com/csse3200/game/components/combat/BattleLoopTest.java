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

  private static CardConfig card(
      String id, CardType type, TargetType target, int cost, EffectConfig... effects) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card";
    card.cost = cost;
    card.type = type;
    card.target = target;
    card.effects = effects;
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }

  private static CardConfig strikeCard() {
    return card(
        "strike",
        CardType.ATTACK,
        TargetType.SINGLE_ENEMY,
        1,
        new EffectConfig(EffectType.DAMAGE, 6));
  }

  private static Entity enemy(String id, int health, int baseAttack) {
    return new Entity()
        .addComponent(new CombatStatsComponent(health, baseAttack))
        .addComponent(new EnemyBehaviourComponent("test"));
  }

  private static BattleDeck deckWithFirstCardInHand(String firstCard, String replacementCard) {
    BattleDeck deck = new BattleDeck(new PlayerDeck(List.of(firstCard, replacementCard)));
    deck.drawCards(1);
    return deck;
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

  @Test
  void shouldAddArmourWhenDefendCardIsPlayed() {
    CardConfig defend =
        card("defend", CardType.SKILL, TargetType.SELF, 1, new EffectConfig(EffectType.BLOCK, 5));
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    BattleDeck deck = deckWithFirstCardInHand("defend", "strike");
    CardLibrary library = new CardLibrary(List.of(defend));
    BattleController controller =
        new BattleController(
            player, List.of(enemy("enemy", 20, 1)), new CardEffectResolver(library), library, deck);
    List<BattlePhase> phases = new ArrayList<>();
    controller.addPhaseChangeListener((previous, next) -> phases.add(next));

    controller.start();
    boolean accepted =
        controller.submitCardPlayRequest(
            new CardPlayRequest("defend", "player"), PlayerIntent.DEFEND);

    assertTrue(accepted);
    assertEquals(5, player.getComponent(CombatStatsComponent.class).getArmor());
    assertEquals(2, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertTrue(deck.getDiscardPile().contains("defend"));
    assertTrue(phases.contains(BattlePhase.PLAYER_DEFEND));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldHealPlayerWhenOtherCardIsPlayed() {
    CardConfig bandage =
        card("bandage", CardType.SKILL, TargetType.SELF, 1, new EffectConfig(EffectType.HEAL, 4));
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0, 20))
            .addComponent(new EnergyComponent(3));
    BattleDeck deck = deckWithFirstCardInHand("bandage", "strike");
    CardLibrary library = new CardLibrary(List.of(bandage));
    BattleController controller =
        new BattleController(
            player, List.of(enemy("enemy", 20, 1)), new CardEffectResolver(library), library, deck);
    List<BattlePhase> phases = new ArrayList<>();
    controller.addPhaseChangeListener((previous, next) -> phases.add(next));

    controller.start();
    boolean accepted =
        controller.submitCardPlayRequest(
            new CardPlayRequest("bandage", "player"), PlayerIntent.OTHER);

    assertTrue(accepted);
    assertEquals(14, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(2, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertTrue(deck.getDiscardPile().contains("bandage"));
    assertTrue(phases.contains(BattlePhase.PLAYER_OTHER));
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldRejectCardOutsidePlayerTurnWithoutChangingState() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy = enemy("enemy", 20, 1);
    BattleDeck deck = deckWithFirstCardInHand("strike", "bandage");
    CardLibrary library = new CardLibrary(List.of(strikeCard()));
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectResolver(library), library, deck);

    boolean accepted =
        controller.submitCardPlayRequest(
            new CardPlayRequest("strike", "enemy"), PlayerIntent.ATTACK);

    assertFalse(accepted);
    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
    assertEquals(20, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(3, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(List.of("strike"), deck.getHand());
    assertTrue(deck.getDiscardPile().isEmpty());
  }

  @Test
  void shouldNotSpendEnergyWhenCardCannotBePlayed() {
    CardConfig expensiveStrike =
        card(
            "strike",
            CardType.ATTACK,
            TargetType.SINGLE_ENEMY,
            4,
            new EffectConfig(EffectType.DAMAGE, 10));
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity enemy = enemy("enemy", 20, 1);
    BattleDeck deck = deckWithFirstCardInHand("strike", "bandage");
    CardLibrary library = new CardLibrary(List.of(expensiveStrike));
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectResolver(library), library, deck);

    controller.start();
    boolean accepted =
        controller.submitCardPlayRequest(
            new CardPlayRequest("strike", "enemy"), PlayerIntent.ATTACK);

    assertFalse(accepted);
    assertEquals(3, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(20, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(List.of("strike"), deck.getHand());
    assertTrue(deck.getDiscardPile().isEmpty());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldApplyAllEnemiesCardToEveryLivingEnemy() {
    CardConfig expose =
        card(
            "expose",
            CardType.SKILL,
            TargetType.ALL_ENEMIES,
            1,
            new EffectConfig(EffectType.VULNERABLE, 2, 2));
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new EnergyComponent(3));
    Entity firstEnemy = enemy("first_enemy", 20, 1);
    Entity secondEnemy = enemy("second_enemy", 20, 1);
    BattleDeck deck = deckWithFirstCardInHand("expose", "strike");
    CardLibrary library = new CardLibrary(List.of(expose));
    BattleController controller =
        new BattleController(
            player,
            List.of(firstEnemy, secondEnemy),
            new CardEffectResolver(library),
            library,
            deck);

    controller.start();
    boolean accepted =
        controller.submitCardPlayRequest(
            new CardPlayRequest("expose", "first_enemy"), PlayerIntent.OTHER);

    assertTrue(accepted);
    assertTrue(firstEnemy.getComponent(CombatStatsComponent.class).hasStatusEffect("vulnerable"));
    assertTrue(secondEnemy.getComponent(CombatStatsComponent.class).hasStatusEffect("vulnerable"));
    assertEquals(2, player.getComponent(EnergyComponent.class).getCurrentEnergy());
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }
}
