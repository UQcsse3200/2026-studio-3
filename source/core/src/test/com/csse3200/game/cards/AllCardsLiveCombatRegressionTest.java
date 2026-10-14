package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.integration.Team1EnemyStateAdapter;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIFactory;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Proves every production card resolves and applies its real effects through BattleController. */
@ExtendWith(GameExtension.class)
class AllCardsLiveCombatRegressionTest {
  private static final String FIRST_TARGET = "enemy-1";
  private static final String SECOND_TARGET = "enemy-2";
  private static final Set<String> EXPECTED_CARD_IDS =
      Set.of(
          "strike",
          "warding_sweep",
          "defend",
          "poison_dagger",
          "expose",
          "inner_focus",
          "bandage",
          "sentinels_rebuke",
          "poison_flask",
          "poison_blade",
          "poison_cloud",
          "poison_mark",
          "starfall",
          "rift_lance",
          "wardens_judgement",
          "unseal_the_breach",
          "astral_ward",
          "resurrection",
          "makeshift_shelter",
          "rubble_barricade",
          "emergency_salve",
          "purify",
          "sealed_pact",
          "blood_price",
          "doom_sigil",
          "iron_oath");

  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void scenarioCoverageShouldExactlyMatchConfiguredAndEligibleCards() {
    Set<String> configured =
        library.getAllCards().stream()
            .map(card -> card.id)
            .collect(java.util.stream.Collectors.toSet());
    Set<String> eligible =
        new HashSet<>(CardAcquisitionPoolLoader.loadDefault(library).eligibleCardIds());

    assertEquals(EXPECTED_CARD_IDS, configured);
    assertEquals(configured, eligible);
  }

  @Test
  void shouldPlayEveryConfiguredCardThroughBattleController() {
    assertAll(EXPECTED_CARD_IDS.stream().sorted().map(cardId -> () -> playAndAssertCard(cardId)));
  }

  private void playAndAssertCard(String cardId) {
    Harness battle = createHarness(List.of(cardId));
    prepareScenario(cardId, battle);
    CardConfig config = library.getCard(cardId).orElseThrow();
    String instanceId = battle.instanceId(cardId);
    int initialEnergy = battle.energy.getCurrentEnergy();

    assertTrue(
        battle.controller.submitCardPlayRequest(requestFor(config.target, instanceId)), cardId);

    int energyGain =
        Stream.of(config.effects)
            .filter(effect -> effect.type == EffectType.ENERGY_GAIN)
            .mapToInt(effect -> effect.value)
            .sum();
    assertEquals(
        Math.min(battle.energy.getMaxEnergy(), initialEnergy - config.cost + energyGain),
        battle.energy.getCurrentEnergy(),
        cardId + " energy");
    assertTrue(
        battle.deck.getHand().stream().noneMatch(card -> card.instanceId().equals(instanceId)),
        cardId + " exact instance should leave hand");
    assertTrue(
        battle.deck.getDiscardPileInstances().stream()
            .anyMatch(card -> card.instanceId().equals(instanceId)),
        cardId + " exact instance should enter discard");
    assertScenarioOutcome(cardId, battle);
  }

  @Test
  void feebleShouldReduceTheAffectedEnemysNextAttack() {
    Harness battle = createHarness(List.of("sentinels_rebuke"));

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("sentinels_rebuke"), FIRST_TARGET)));
    battle.controller.endPlayerTurn();

    assertEquals(
        79,
        battle.playerStats.getHealth(),
        "affected enemy should deal 9 damage while the unaffected enemy still deals 12");
  }

  @Test
  void strengthShouldIncreaseTheNextCardThroughTheSameController() {
    Harness battle = createHarness(List.of("inner_focus", "strike"));

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.self(battle.instanceId("inner_focus"))));
    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("strike"), FIRST_TARGET)));

    assertEquals(492, battle.firstEnemyStats.getHealth());
  }

  @Test
  void vulnerableShouldIncreaseOnlyTheFollowUpAttack() {
    Harness battle = createHarness(List.of("rift_lance", "strike"));

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("rift_lance"), FIRST_TARGET)));
    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("strike"), FIRST_TARGET)));

    assertEquals(487, battle.firstEnemyStats.getHealth());
  }

  @Test
  void playerFeebleShouldReduceOutgoingCardDamageExactlyOnce() {
    Harness battle = createHarness(List.of("strike"));
    battle.playerStats.applyStatusEffect(EffectType.FEEBLE.name(), 1, 2);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("strike"), FIRST_TARGET)));

    assertEquals(496, battle.firstEnemyStats.getHealth());
  }

  @Test
  void playerVulnerableShouldIncreaseIncomingEnemyDamageExactlyOnce() {
    Harness battle = createHarness(List.of("defend"));
    battle.playerStats.applyStatusEffect(EffectType.VULNERABLE.name(), 1, 2);

    battle.controller.endPlayerTurn();

    assertEquals(64, battle.playerStats.getHealth());
  }

  @Test
  void immediateHealingCardsShouldRespectMaximumHealth() {
    Harness bandage = createHarness(List.of("bandage"), 198, 0);
    Harness shelter = createHarness(List.of("makeshift_shelter"), 199, 0);
    Harness salve = createHarness(List.of("emergency_salve"), 195, 0);

    assertTrue(
        bandage.controller.submitCardPlayRequest(
            CardPlayRequest.self(bandage.instanceId("bandage"))));
    assertTrue(
        shelter.controller.submitCardPlayRequest(
            CardPlayRequest.self(shelter.instanceId("makeshift_shelter"))));
    assertTrue(
        salve.controller.submitCardPlayRequest(
            CardPlayRequest.self(salve.instanceId("emergency_salve"))));

    assertEquals(200, bandage.playerStats.getHealth());
    assertEquals(200, shelter.playerStats.getHealth());
    assertEquals(200, salve.playerStats.getHealth());
  }

  @Test
  void blockShouldAbsorbTheCurrentEnemyPhaseThenResetAtNextPlayerStart() {
    Harness battle = createHarness(List.of("defend"));

    assertTrue(
        battle.controller.submitCardPlayRequest(CardPlayRequest.self(battle.instanceId("defend"))));
    battle.controller.endPlayerTurn();

    assertEquals(81, battle.playerStats.getHealth());
    assertEquals(0, battle.playerStats.getBlock());
  }

  @Test
  void ironOathArmourShouldSurviveTheTurnBoundaryWhenNotFullyConsumed() {
    Harness battle = createHarness(List.of("iron_oath"), 100, 1);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.self(battle.instanceId("iron_oath"))));
    battle.controller.endPlayerTurn();

    assertEquals(100, battle.playerStats.getHealth());
    assertEquals(2, battle.playerStats.getArmour());
    assertEquals(0, battle.playerStats.getBlock());
  }

  @Test
  void poisonShouldTickAsPiercingDamageAndExpireThroughTheBattleLoop() {
    Harness battle = createHarness(List.of("poison_flask"), 100, 0);
    battle.firstEnemyStats.setBlock(20);
    battle.firstEnemyStats.setArmour(20);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("poison_flask"), FIRST_TARGET)));

    battle.controller.endPlayerTurn();
    assertEquals(495, battle.firstEnemyStats.getHealth());
    assertStatus(battle.firstEnemyStats, EffectType.POISON, 5, 2);
    assertEquals(20, battle.firstEnemyStats.getBlock());
    assertEquals(20, battle.firstEnemyStats.getArmour());

    battle.controller.endPlayerTurn();
    assertEquals(490, battle.firstEnemyStats.getHealth());
    assertStatus(battle.firstEnemyStats, EffectType.POISON, 5, 1);

    battle.controller.endPlayerTurn();
    assertEquals(485, battle.firstEnemyStats.getHealth());
    assertNull(battle.firstEnemyStats.getStatusEffect(EffectType.POISON.name()));
  }

  @Test
  void poisonApplicationsShouldStackWhileKeepingIndependentDurations() {
    Harness battle = createHarness(List.of("poison_mark", "poison_flask"), 100, 0);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("poison_mark"), FIRST_TARGET)));
    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("poison_flask"), FIRST_TARGET)));

    battle.controller.endPlayerTurn();
    assertEquals(493, battle.firstEnemyStats.getHealth());
    assertStatus(battle.firstEnemyStats, EffectType.POISON, 7, 2);

    battle.controller.endPlayerTurn();
    assertEquals(486, battle.firstEnemyStats.getHealth());
    assertStatus(battle.firstEnemyStats, EffectType.POISON, 5, 1);

    battle.controller.endPlayerTurn();
    assertEquals(481, battle.firstEnemyStats.getHealth());
    assertNull(battle.firstEnemyStats.getStatusEffect(EffectType.POISON.name()));
  }

  @Test
  void feebleReapplicationShouldNotStackItsReductionOrShortenDuration() {
    Harness battle = createHarness(List.of("sentinels_rebuke", "sentinels_rebuke"));

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("sentinels_rebuke"), FIRST_TARGET)));
    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("sentinels_rebuke"), FIRST_TARGET)));

    assertStatus(battle.firstEnemyStats, EffectType.FEEBLE, 1, 2);
  }

  @Test
  void resurrectionShouldHealAtThreeFuturePlayerStartsThenExpire() {
    Harness battle = createHarness(List.of("resurrection"), 50, 0);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.self(battle.instanceId("resurrection"))));
    assertEquals(50, battle.playerStats.getHealth());

    battle.controller.endPlayerTurn();
    assertEquals(56, battle.playerStats.getHealth());
    assertStatus(battle.playerStats, EffectType.HEAL, 6, 2);

    battle.controller.endPlayerTurn();
    assertEquals(62, battle.playerStats.getHealth());
    assertStatus(battle.playerStats, EffectType.HEAL, 6, 1);

    battle.controller.endPlayerTurn();
    assertEquals(68, battle.playerStats.getHealth());
    assertNull(battle.playerStats.getStatusEffect(EffectType.HEAL.name()));
  }

  @Test
  void resurrectionShouldNotHealOrRemainAfterDefeat() {
    Harness battle = createHarness(List.of("resurrection"), 50, 0);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.self(battle.instanceId("resurrection"))));
    battle.playerStats.setHealth(0);
    battle.controller.endPlayerTurn();

    assertEquals(0, battle.playerStats.getHealth());
    assertNull(battle.playerStats.getStatusEffect(EffectType.HEAL.name()));
  }

  @Test
  void sunderShouldClampArmourAtZeroBeforeApplyingFollowingDamage() {
    Harness battle = createHarness(List.of("unseal_the_breach"));
    battle.firstEnemyStats.setArmour(2);

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(battle.instanceId("unseal_the_breach"), FIRST_TARGET)));

    assertEquals(0, battle.firstEnemyStats.getArmour());
    assertEquals(498, battle.firstEnemyStats.getHealth());
  }

  @Test
  void rejectedRequestShouldNotSpendMoveOrApplyTheSelectedCard() {
    Harness battle = createHarness(List.of("resurrection"));
    String instanceId = battle.instanceId("resurrection");
    battle.energy.setCurrentEnergy(0);

    assertFalse(battle.controller.submitCardPlayRequest(CardPlayRequest.self(instanceId)));
    assertEquals(0, battle.energy.getCurrentEnergy());
    assertTrue(
        battle.deck.getHand().stream().anyMatch(card -> card.instanceId().equals(instanceId)));
    assertTrue(
        battle.deck.getDiscardPileInstances().stream()
            .noneMatch(card -> card.instanceId().equals(instanceId)));
    assertNull(battle.playerStats.getStatusEffect(EffectType.HEAL.name()));
  }

  @Test
  void playingOneDuplicateShouldLeaveTheOtherExactInstanceInHand() {
    Harness battle = createHarness(List.of("strike", "strike"));
    List<String> instanceIds =
        battle.deck.getHand().stream().map(CardInstance::instanceId).toList();

    assertTrue(
        battle.controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(instanceIds.get(0), FIRST_TARGET)));

    assertTrue(
        battle.deck.getHand().stream()
            .anyMatch(card -> card.instanceId().equals(instanceIds.get(1))));
    assertTrue(
        battle.deck.getDiscardPileInstances().stream()
            .anyMatch(card -> card.instanceId().equals(instanceIds.get(0))));
  }

  private Harness createHarness(List<String> cardIds) {
    return createHarness(cardIds, 100, 12);
  }

  private Harness createHarness(List<String> cardIds, int playerHealth, int enemyAttack) {
    CombatStatsComponent playerStats = new CombatStatsComponent(playerHealth, 0, 200);
    EnergyComponent energy = new EnergyComponent(10);
    Entity player = new Entity().addComponent(playerStats).addComponent(energy);

    CombatStatsComponent firstStats = new CombatStatsComponent(500, enemyAttack);
    CombatStatsComponent secondStats = new CombatStatsComponent(500, enemyAttack);
    Entity firstEnemy = enemy(firstStats);
    Entity secondEnemy = enemy(secondStats);
    Map<String, Entity> enemies = new LinkedHashMap<>();
    enemies.put(FIRST_TARGET, firstEnemy);
    enemies.put(SECOND_TARGET, secondEnemy);

    PlayerDeck playerDeck = new PlayerDeck(library, cardIds);
    BattleDeck deck = new BattleDeck(playerDeck);
    deck.drawCards(cardIds.size());
    Team7PlayerStateAdapter playerState = new Team7PlayerStateAdapter(energy, playerStats);
    Team1EnemyStateAdapter enemyState = new Team1EnemyStateAdapter(enemies);
    CardPlayService playService =
        new CardPlayService(library, deck, energy, playerState, enemyState);
    CardEffectHandler effectHandler = new CardEffectHandler(enemies);
    BattleController controller =
        new BattleController(player, List.of(firstEnemy, secondEnemy), effectHandler, playService);
    controller.start();
    return new Harness(controller, deck, energy, playerStats, firstStats, secondStats);
  }

  private Entity enemy(CombatStatsComponent stats) {
    return new Entity()
        .addComponent(stats)
        .addComponent(new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND));
  }

  private CardPlayRequest requestFor(TargetType target, String instanceId) {
    return switch (target) {
      case SELF -> CardPlayRequest.self(instanceId);
      case SINGLE_ENEMY -> CardPlayRequest.singleEnemy(instanceId, FIRST_TARGET);
      case ALL_ENEMIES -> CardPlayRequest.allEnemies(instanceId);
    };
  }

  private void prepareScenario(String cardId, Harness battle) {
    switch (cardId) {
      case "poison_blade" -> {
        battle.firstEnemyStats.setBlock(8);
        battle.firstEnemyStats.setArmour(8);
      }
      case "unseal_the_breach" -> battle.firstEnemyStats.setArmour(5);
      case "purify" -> {
        battle.playerStats.applyStatusEffect(EffectType.POISON.name(), 3, 2);
        battle.playerStats.applyStatusEffect(EffectType.VULNERABLE.name(), 1, 2);
        battle.playerStats.applyStatusEffect(EffectType.FEEBLE.name(), 1, 2);
        battle.playerStats.applyStatusEffect(EffectType.STRENGTH.name(), 2, 0);
      }
      default -> {
        // No special precondition.
      }
    }
  }

  private void assertScenarioOutcome(String cardId, Harness battle) {
    switch (cardId) {
      case "strike" -> assertEnemyHealth(battle, 494, 500);
      case "warding_sweep" -> assertEnemyHealth(battle, 496, 496);
      case "defend" -> assertEquals(5, battle.playerStats.getBlock());
      case "poison_dagger" -> {
        assertEnemyHealth(battle, 496, 500);
        assertStatus(battle.firstEnemyStats, EffectType.POISON, 3, 3);
      }
      case "expose" -> {
        assertStatus(battle.firstEnemyStats, EffectType.VULNERABLE, 2, 2);
        assertStatus(battle.secondEnemyStats, EffectType.VULNERABLE, 2, 2);
      }
      case "inner_focus" -> assertStatus(battle.playerStats, EffectType.STRENGTH, 2, 0);
      case "bandage" -> assertEquals(106, battle.playerStats.getHealth());
      case "sentinels_rebuke" -> {
        assertEnemyHealth(battle, 496, 500);
        assertStatus(battle.firstEnemyStats, EffectType.FEEBLE, 1, 2);
      }
      case "poison_flask" -> assertStatus(battle.firstEnemyStats, EffectType.POISON, 5, 3);
      case "poison_blade" -> {
        assertEnemyHealth(battle, 490, 500);
        assertEquals(8, battle.firstEnemyStats.getBlock());
        assertEquals(8, battle.firstEnemyStats.getArmour());
        assertStatus(battle.firstEnemyStats, EffectType.POISON, 4, 2);
      }
      case "poison_cloud" -> {
        assertStatus(battle.firstEnemyStats, EffectType.POISON, 3, 3);
        assertStatus(battle.secondEnemyStats, EffectType.POISON, 3, 3);
      }
      case "poison_mark" -> {
        assertStatus(battle.firstEnemyStats, EffectType.VULNERABLE, 2, 2);
        assertStatus(battle.firstEnemyStats, EffectType.POISON, 2, 2);
      }
      case "starfall" -> assertEnemyHealth(battle, 494, 494);
      case "rift_lance" -> {
        assertEnemyHealth(battle, 496, 500);
        assertStatus(battle.firstEnemyStats, EffectType.VULNERABLE, 1, 2);
      }
      case "wardens_judgement" -> assertEnemyHealth(battle, 491, 500);
      case "unseal_the_breach" -> {
        assertEnemyHealth(battle, 500, 500);
        assertEquals(0, battle.firstEnemyStats.getArmour());
      }
      case "astral_ward" -> {
        assertEquals(4, battle.playerStats.getBlock());
        assertStatus(battle.playerStats, EffectType.STRENGTH, 1, 0);
      }
      case "resurrection" -> {
        assertEquals(100, battle.playerStats.getHealth());
        assertStatus(battle.playerStats, EffectType.HEAL, 6, 3);
      }
      case "makeshift_shelter" -> {
        assertEquals(7, battle.playerStats.getBlock());
        assertEquals(103, battle.playerStats.getHealth());
      }
      case "rubble_barricade" -> assertEquals(12, battle.playerStats.getBlock());
      case "emergency_salve" -> assertEquals(110, battle.playerStats.getHealth());
      case "purify" -> {
        assertNull(battle.playerStats.getStatusEffect(EffectType.POISON.name()));
        assertNull(battle.playerStats.getStatusEffect(EffectType.VULNERABLE.name()));
        assertNull(battle.playerStats.getStatusEffect(EffectType.FEEBLE.name()));
        assertNotNull(battle.playerStats.getStatusEffect(EffectType.STRENGTH.name()));
      }
      case "sealed_pact" -> {
        assertStatus(battle.playerStats, EffectType.STRENGTH, 3, 0);
        assertEquals(6, battle.playerStats.getBlock());
      }
      case "blood_price" -> assertEnemyHealth(battle, 480, 500);
      case "doom_sigil" -> {
        assertStatus(battle.firstEnemyStats, EffectType.VULNERABLE, 1, 2);
        assertStatus(battle.firstEnemyStats, EffectType.POISON, 4, 3);
      }
      case "iron_oath" -> {
        assertEquals(4, battle.playerStats.getArmour());
        assertEquals(0, battle.playerStats.getBlock());
      }
      default -> throw new AssertionError("Missing scenario assertions for " + cardId);
    }
  }

  private static void assertEnemyHealth(Harness battle, int first, int second) {
    assertEquals(first, battle.firstEnemyStats.getHealth());
    assertEquals(second, battle.secondEnemyStats.getHealth());
  }

  private static void assertStatus(
      CombatStatsComponent stats, EffectType type, int value, int duration) {
    StatusEffect status = stats.getStatusEffect(type.name());
    assertNotNull(status, type.name());
    assertEquals(value, status.getValue(), type.name() + " value");
    assertEquals(duration, status.getDuration(), type.name() + " duration");
  }

  private record Harness(
      BattleController controller,
      BattleDeck deck,
      EnergyComponent energy,
      CombatStatsComponent playerStats,
      CombatStatsComponent firstEnemyStats,
      CombatStatsComponent secondEnemyStats) {
    String instanceId(String cardId) {
      return deck.getHand().stream()
          .filter(card -> card.cardId().equals(cardId))
          .map(CardInstance::instanceId)
          .findFirst()
          .orElseThrow();
    }
  }
}
