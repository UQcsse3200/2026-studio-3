package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyStatsComponentTest {

  /** Builds an enemy carrying shared combat stats alongside the enemy-specific component. */
  private static Entity enemyWith(CombatStatsComponent stats, EnemyStatsComponent enemyStats) {
    Entity enemy = new Entity();
    enemy.addComponent(stats);
    enemy.addComponent(enemyStats);
    enemy.create();
    return enemy;
  }

  @Test
  void shouldKeepTheDisplayNameGivenToConstructor() {
    EnemyStatsComponent enemyStats = new EnemyStatsComponent("Lesser Shade");

    assertEquals("Lesser Shade", enemyStats.getDisplayName());
  }

  @Test
  void shouldFallBackToADefaultDisplayName() {
    assertEquals("Unknown Enemy", new EnemyStatsComponent().getDisplayName());
  }

  @Test
  void shouldFallBackToADefaultDisplayNameWhenNull() {
    assertEquals("Unknown Enemy", new EnemyStatsComponent(null).getDisplayName());
  }

  @Test
  void shouldFallBackToADefaultDisplayNameWhenBlank() {
    assertEquals("Unknown Enemy", new EnemyStatsComponent("   ").getDisplayName());
  }

  @Test
  void shouldTriggerEnemyDamagedWithTheHealthActuallyLost() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDamaged", listener);

    stats.takeDamage(8);

    verify(listener).handle(8);
  }

  @Test
  void shouldReportOnlyHealthLostAfterArmourAbsorbs() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    stats.setArmour(5);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDamaged", listener);

    stats.takeDamage(8);

    verify(listener).handle(3);
  }

  @Test
  void shouldTriggerEnemyDefeatedWhenHealthReachesZero() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyDefeated", listener);

    stats.takeDamage(20);

    verify(listener).handle();
  }

  @Test
  void shouldNotTriggerEnemyDamagedWhenHealingRaisesHealth() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    stats.setHealth(10);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDamaged", listener);

    stats.heal(5);

    verifyNoInteractions(listener);
  }

  @Test
  void shouldNotTriggerEnemyDefeatedTwice() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyDefeated", listener);

    stats.takeDamage(20);
    stats.takeDamage(5);

    verify(listener).handle();
  }

  @Test
  void shouldSurviveCreationWithoutCombatStats() {
    Entity enemy = new Entity();
    enemy.addComponent(new EnemyStatsComponent("Lesser Shade"));

    enemy.create();

    assertEquals("Lesser Shade", enemy.getComponent(EnemyStatsComponent.class).getDisplayName());
  }

  // 血量跌破阈值（30% 上限）时应该广播一次 enemyEnraged
  @Test
  void shouldTriggerEnemyEnragedWhenHealthDropsBelowThreshold() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", listener);

    stats.takeDamage(15); // 20 -> 5，低于 20*0.3=6

    verify(listener, times(1)).handle();
  }

  // enemyEnraged 只应该触发一次，之后即使继续掉血也不会重复广播
  @Test
  void shouldOnlyTriggerEnemyEnragedOnce() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", listener);

    stats.takeDamage(15); // 触发一次
    stats.takeDamage(1); // 还活着，仍然低于阈值，不应该再触发

    verify(listener, times(1)).handle();
  }

  // 被打死的那一击不应该触发 enemyEnraged（应该走 enemyDefeated）
  @Test
  void shouldNotTriggerEnemyEnragedOnKillingBlow() {
    CombatStatsComponent stats = new CombatStatsComponent(20, 6);
    Entity enemy = enemyWith(stats, new EnemyStatsComponent("Lesser Shade"));

    EventListener0 enragedListener = mock(EventListener0.class);
    EventListener0 defeatedListener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", enragedListener);
    enemy.getEvents().addListener("enemyDefeated", defeatedListener);

    stats.takeDamage(20); // 直接打死

    verifyNoInteractions(enragedListener);
    verify(defeatedListener).handle();
  }
}
