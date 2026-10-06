package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAI;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIContext;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIFactory;
import com.csse3200.game.components.enemy.Memory.EnemyMemoryComponent;
import com.csse3200.game.components.enemy.Memory.PlayerMemory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

@ExtendWith(GameExtension.class)
class EnemyBehaviourComponentTest {

  /** Builds an enemy with the given combat stats, or with no stats component at all. */
  private static Entity enemyWith(EnemyBehaviourComponent behaviour, CombatStatsComponent stats) {
    Entity enemy = new Entity();
    if (stats != null) {
      enemy.addComponent(stats);
    }
    enemy.addComponent(behaviour);
    enemy.create();
    return enemy;
  }

  /** Combat stats matching the enemy previously used across these tests. */
  private static CombatStatsComponent enemyStats() {
    return new CombatStatsComponent(20, 6);
  }

  /** An AI that always returns the same intent, so intent resolution can be tested directly. */
  private static EnemyAI fixedAi(EnemyIntent intent) {
    return context -> intent;
  }

  @Test
  void shouldKeepBehaviourIdGivenToConstructor() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);

    assertEquals(EnemyAIFactory.CYCLE_ATTACK_DEFEND, behaviour.getBehaviourId());
  }

  @Test
  void shouldStartWithUnknownIntent() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);

    assertEquals(IntentType.UNKNOWN, behaviour.getCurrentIntent().getType());
  }

  @Test
  void shouldAttackOnTheFirstTurn() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, enemyStats());

    EnemyIntent intent = behaviour.rollIntent();

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(6, intent.getValue());
  }

  @Test
  void shouldDefendOnTheSecondTurn() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, enemyStats());

    behaviour.rollIntent();
    EnemyIntent intent = behaviour.rollIntent();

    assertEquals(IntentType.DEFEND, intent.getType());
  }

  @Test
  void shouldExposeTheRolledIntentAsCurrent() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, enemyStats());

    EnemyIntent rolled = behaviour.rollIntent();

    assertSame(rolled, behaviour.getCurrentIntent());
  }

  @Test
  void shouldTelegraphTheIntentToListeners() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    Entity enemy = enemyWith(behaviour, enemyStats());

    @SuppressWarnings("unchecked")
    EventListener1<EnemyIntent> listener = (EventListener1<EnemyIntent>) mock(EventListener1.class);
    enemy.getEvents().addListener("intentChanged", listener);

    behaviour.rollIntent();

    verify(listener).handle(any(EnemyIntent.class));
  }

  @Test
  void shouldRollUnknownWithoutStatsComponent() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, null);

    EnemyIntent intent = behaviour.rollIntent();

    assertEquals(IntentType.UNKNOWN, intent.getType());
  }

  @Test
  void shouldFallBackToADefaultBehaviourForUnknownId() {
    EnemyBehaviourComponent behaviour = new EnemyBehaviourComponent("no_such_behaviour");
    enemyWith(behaviour, enemyStats());

    EnemyIntent intent = behaviour.rollIntent();

    assertNotNull(intent);
    assertEquals(IntentType.ATTACK, intent.getType());
  }

  @Test
  void shouldGainArmourWhenResolvingDefend() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    CombatStatsComponent stats = enemyStats();
    enemyWith(behaviour, stats);

    behaviour.rollIntent();
    behaviour.rollIntent();
    behaviour.executeIntent(null);

    assertEquals(2, stats.getArmour());
  }

  @Test
  void shouldDamageTheTargetWhenResolvingAttack() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, enemyStats());

    Entity player = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    player.addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertEquals(24, playerStats.getHealth());
  }

  @Test
  void shouldNotifyAttackOnceBeforeTheExistingDamageCall() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent("test_attack", fixedAi(EnemyIntent.attack(11)));
    Entity enemy = enemyWith(behaviour, enemyStats());
    CombatStatsComponent playerStats = spy(new CombatStatsComponent(30, 4));
    playerStats.setBlock(2);
    playerStats.setArmour(3);
    Entity player = new Entity().addComponent(playerStats);
    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyAttack", listener);
    enemy.getEvents().addListener("enemyAttack", () -> assertEquals(30, playerStats.getHealth()));

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    InOrder order = inOrder(listener, playerStats);
    order.verify(listener).handle();
    order.verify(playerStats).takeDamage(11);
    verify(listener, times(1)).handle();
    verify(playerStats, times(1)).takeDamage(11);
    assertEquals(24, playerStats.getHealth());
    assertEquals(0, playerStats.getBlock());
    assertEquals(0, playerStats.getArmour());
  }

  @Test
  void shouldNotNotifyAttackForMissingTargetStatsOrDefending() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    Entity enemy = enemyWith(behaviour, enemyStats());
    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyAttack", listener);

    behaviour.rollIntent();
    behaviour.executeIntent(null);
    behaviour.executeIntent(new Entity());
    behaviour.rollIntent();
    behaviour.executeIntent(new Entity().addComponent(new CombatStatsComponent(30, 0)));

    verify(listener, never()).handle();
  }

  @Test
  void shouldApplyStrengthFeebleAndTargetVulnerableToAttackDamageOnce() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    CombatStatsComponent attackerStats = enemyStats();
    attackerStats.applyStatusEffect(EffectType.STRENGTH.name(), 2, 0);
    attackerStats.applyStatusEffect(EffectType.FEEBLE.name(), 1, 2);
    enemyWith(behaviour, attackerStats);

    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    playerStats.applyStatusEffect(EffectType.VULNERABLE.name(), 1, 2);
    Entity player = new Entity().addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertEquals(21, playerStats.getHealth());
  }

  @Test
  void shouldApplyFeebleOnceAndRoundFinalDamageDownWithoutChangingIntent() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent("test_feeble", fixedAi(EnemyIntent.attack(7)));
    CombatStatsComponent attackerStats = enemyStats();
    attackerStats.applyStatusEffect(EffectType.FEEBLE.name(), 1, 2);
    enemyWith(behaviour, attackerStats);

    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    Entity player = new Entity().addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertEquals(25, playerStats.getHealth());
    assertEquals(7, behaviour.getCurrentIntent().getValue());
  }

  @Test
  void shouldDealDamageEqualToIntentValueNotJustBaseAttack() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent("test_bonus", fixedAi(EnemyIntent.attack(11)));
    enemyWith(behaviour, enemyStats()); // baseAttack = 6

    Entity player = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    player.addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertEquals(19, playerStats.getHealth()); // 30 - 11，而不是 30 - 6
  }

  @Test
  void shouldIgnoreAttackAgainstNullTarget() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    enemyWith(behaviour, enemyStats());

    behaviour.rollIntent();

    behaviour.executeIntent(null);
  }

  @Test
  void shouldApplyTheStatusEffectWhenResolvingDebuff() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_debuff", fixedAi(EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 2)));
    enemyWith(behaviour, enemyStats());

    Entity player = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    player.addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertTrue(playerStats.hasStatusEffect("SILENCE"));
  }

  @Test
  void shouldPassTheEffectNameValueAndDurationToTheTarget() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_debuff", fixedAi(EnemyIntent.debuff(IntentEffectType.SILENCE, 3, 2)));
    enemyWith(behaviour, enemyStats());

    Entity player = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    player.addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    StatusEffect applied = playerStats.getStatusEffect("SILENCE");
    assertNotNull(applied);
    assertEquals("SILENCE", applied.getType());
    assertEquals(3, applied.getValue());
    assertEquals(2, applied.getDuration());
  }

  @Test
  void shouldStoreTauntUnderTheCasterIdWithTheCasterAsItsValue() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_taunt", fixedAi(EnemyIntent.debuff(IntentEffectType.TAUNT, 0, 2)));
    Entity enemy = enemyWith(behaviour, enemyStats());
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    Entity player = new Entity().addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    StatusEffect applied = playerStats.getStatusEffect("TAUNT:" + enemy.getId());
    assertNotNull(applied);
    assertEquals(enemy.getId(), applied.getValue());
    assertEquals(2, applied.getDuration());
  }

  @Test
  void shouldIgnoreDebuffAgainstNullTarget() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_debuff", fixedAi(EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 2)));
    enemyWith(behaviour, enemyStats());

    behaviour.rollIntent();

    behaviour.executeIntent(null);
  }

  @Test
  void shouldIgnoreADebuffIntentCarryingNoEffectType() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent("test_debuff", fixedAi(new EnemyIntent(IntentType.DEBUFF, 1)));
    enemyWith(behaviour, enemyStats());

    Entity player = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(30, 4);
    player.addComponent(playerStats);
    player.create();

    behaviour.rollIntent();
    behaviour.executeIntent(player);

    assertNull(playerStats.getStatusEffect("SILENCE"));
  }

  @Test
  void shouldReportUnknownPlayerHealthUntilPlayerStatsAreSupplied() {
    EnemyAIContext[] captured = new EnemyAIContext[1];
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_capture",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });
    enemyWith(behaviour, enemyStats());

    behaviour.rollIntent();

    assertEquals(0, captured[0].getPlayerHealth());
  }

  @Test
  void shouldReportThePlayerHealthOnceSupplied() {
    EnemyAIContext[] captured = new EnemyAIContext[1];
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_capture",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });
    enemyWith(behaviour, enemyStats());

    behaviour.setPlayerStats(new CombatStatsComponent(30, 4));
    behaviour.rollIntent();

    assertEquals(30, captured[0].getPlayerHealth());
  }

  @Test
  void shouldReportUnknownPlayerHealthWhenPlayerStatsAreCleared() {
    EnemyAIContext[] captured = new EnemyAIContext[1];
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_capture",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });
    enemyWith(behaviour, enemyStats());

    behaviour.setPlayerStats(new CombatStatsComponent(30, 4));
    behaviour.setPlayerStats(null);
    behaviour.rollIntent();

    assertEquals(0, captured[0].getPlayerHealth());
  }

  @Test
  void shouldUseEmptyPlayerMemoryWhenMemoryIsNotSupplied() {
    EnemyAIContext[] captured = new EnemyAIContext[1];

    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_memory",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });

    enemyWith(behaviour, enemyStats());

    behaviour.rollIntent();

    assertEquals(PlayerMemory.empty(), captured[0].getPlayerMemory());
  }

  @Test
  void shouldProvidePlayerMemoryToEnemyAI() {
    EnemyAIContext[] captured = new EnemyAIContext[1];

    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_memory",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });

    enemyWith(behaviour, enemyStats());

    EnemyMemoryComponent memory = new EnemyMemoryComponent();

    memory.recordCardPlayed(CardType.ATTACK, false);

    memory.recordCardPlayed(CardType.SKILL, true);

    memory.settlePlayerTurn();

    behaviour.setEnemyMemory(memory);
    behaviour.rollIntent();

    PlayerMemory received = captured[0].getPlayerMemory();

    assertEquals(memory.snapshot(), received);
    assertEquals(1, received.attackCardsPlayed());
    assertEquals(1, received.skillCardsPlayed());
    assertEquals(2, received.cardsPlayedLastTurn());
    assertEquals(0, received.consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldReadLatestMemoryForEveryDecision() {
    EnemyAIContext[] captured = new EnemyAIContext[1];

    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_memory",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });

    enemyWith(behaviour, enemyStats());

    EnemyMemoryComponent memory = new EnemyMemoryComponent();

    behaviour.setEnemyMemory(memory);

    memory.recordCardPlayed(CardType.ATTACK, false);

    behaviour.rollIntent();

    PlayerMemory firstDecisionMemory = captured[0].getPlayerMemory();

    assertEquals(1, firstDecisionMemory.attackCardsPlayed());

    assertEquals(0, firstDecisionMemory.skillCardsPlayed());

    memory.recordCardPlayed(CardType.SKILL, true);

    memory.settlePlayerTurn();

    behaviour.rollIntent();

    PlayerMemory secondDecisionMemory = captured[0].getPlayerMemory();

    assertEquals(1, secondDecisionMemory.attackCardsPlayed());

    assertEquals(1, secondDecisionMemory.skillCardsPlayed());

    assertEquals(2, secondDecisionMemory.cardsPlayedLastTurn());

    assertEquals(0, secondDecisionMemory.consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldReturnToEmptyMemoryWhenMemoryIsCleared() {
    EnemyAIContext[] captured = new EnemyAIContext[1];

    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(
            "test_memory",
            context -> {
              captured[0] = context;
              return EnemyIntent.attack(1);
            });

    enemyWith(behaviour, enemyStats());

    EnemyMemoryComponent memory = new EnemyMemoryComponent();

    memory.recordCardPlayed(CardType.ATTACK, false);

    behaviour.setEnemyMemory(memory);
    behaviour.setEnemyMemory(null);
    behaviour.rollIntent();

    assertEquals(PlayerMemory.empty(), captured[0].getPlayerMemory());
  }
}
