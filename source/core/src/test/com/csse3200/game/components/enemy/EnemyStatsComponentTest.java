package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyStatsComponentTest {
  @Test
  void shouldApplyDamageToArmourBeforeHealth() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(8);

    assertEquals(17, stats.getHealth());
    assertEquals(0, stats.getArmour());
  }

  @Test
  void shouldLetArmourFullyAbsorbDamage() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(3);

    assertEquals(20, stats.getHealth());
    assertEquals(2, stats.getArmour());
  }

  @Test
  void shouldDamageHealthDirectlyWhenNoArmour() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(6);

    assertEquals(14, stats.getHealth());
    assertEquals(0, stats.getArmour());
  }

  @Test
  void shouldIgnoreZeroDamage() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(0);

    assertEquals(20, stats.getHealth());
    assertEquals(5, stats.getArmour());
  }

  @Test
  void shouldIgnoreNegativeDamage() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(-5);

    assertEquals(20, stats.getHealth());
    assertEquals(5, stats.getArmour());
  }

  @Test
  void shouldNotReduceHealthBelowZero() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    stats.takeDamage(999);

    assertEquals(0, stats.getHealth());
  }

  @Test
  void shouldTriggerEnemyDefeatedWhenHealthReachesZero() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyDefeated", listener);

    stats.takeDamage(20);

    verify(listener).handle();
  }

  @Test
  void shouldTriggerEnemyDamagedWithActualHealthDamage() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDamaged", listener);

    stats.takeDamage(8);

    verify(listener).handle(3);
    assertEquals(17, stats.getHealth());
    assertEquals(0, stats.getArmour());
  }

  @Test
  void shouldClampSetArmourToZero() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.setArmour(-10);

    assertEquals(0, stats.getArmour());
  }

  @Test
  void shouldClampAddArmourToZero() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    stats.addArmour(-10);

    assertEquals(0, stats.getArmour());
  }

  // 护甲真的增加时才应该广播 enemyDefended
  @Test
  void shouldTriggerEnemyDefendedWhenArmourIncreases() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDefended", listener);

    stats.addArmour(4);

    verify(listener).handle(4);
  }

  // 护甲被削减（负数）时不应该广播 enemyDefended
  @Test
  void shouldNotTriggerEnemyDefendedWhenArmourDecreases() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 5);
    enemy.addComponent(stats);
    enemy.create();

    @SuppressWarnings("unchecked")
    EventListener1<Integer> listener = (EventListener1<Integer>) mock(EventListener1.class);
    enemy.getEvents().addListener("enemyDefended", listener);

    stats.addArmour(-3);

    verifyNoInteractions(listener);
  }

  // 血量跌破阈值（30% 上限）时应该广播一次 enemyEnraged
  @Test
  void shouldTriggerEnemyEnragedWhenHealthDropsBelowThreshold() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", listener);

    stats.takeDamage(15); // 20 -> 5，低于 20*0.3=6

    verify(listener, times(1)).handle();
  }

  // enemyEnraged 只应该触发一次，之后即使继续掉血也不会重复广播
  @Test
  void shouldOnlyTriggerEnemyEnragedOnce() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    EventListener0 listener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", listener);

    stats.takeDamage(15); // 触发一次
    stats.takeDamage(1); // 还活着，仍然低于阈值，不应该再触发

    verify(listener, times(1)).handle();
  }

  // 被打死的那一击不应该触发 enemyEnraged（应该走 enemyDefeated）
  @Test
  void shouldNotTriggerEnemyEnragedOnKillingBlow() {
    Entity enemy = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    enemy.addComponent(stats);
    enemy.create();

    EventListener0 enragedListener = mock(EventListener0.class);
    EventListener0 defeatedListener = mock(EventListener0.class);
    enemy.getEvents().addListener("enemyEnraged", enragedListener);
    enemy.getEvents().addListener("enemyDefeated", defeatedListener);

    stats.takeDamage(20); // 直接打死

    verifyNoInteractions(enragedListener);
    verify(defeatedListener).handle();
  }
}
