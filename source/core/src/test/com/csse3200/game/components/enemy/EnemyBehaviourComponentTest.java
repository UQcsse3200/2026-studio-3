package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAI;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIContext;
import com.csse3200.game.components.enemy.EnemyAI.EnemyAIFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
  void shouldGainArmorWhenResolvingDefend() {
    EnemyBehaviourComponent behaviour =
        new EnemyBehaviourComponent(EnemyAIFactory.CYCLE_ATTACK_DEFEND);
    CombatStatsComponent stats = enemyStats();
    enemyWith(behaviour, stats);

    behaviour.rollIntent();
    behaviour.rollIntent();
    behaviour.executeIntent(null);

    assertEquals(2, stats.getArmor());
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
}
