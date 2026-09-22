package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.enemy.EnemyIntent;
import org.junit.jupiter.api.Test;

class EnemyAIContextTest {
  @Test
  void shouldStoreBattleInformation() {
    EnemyIntent previousIntent = EnemyIntent.defend(4);

    EnemyAIContext context = new EnemyAIContext(80, 20, 30, 7, 3, previousIntent, 2);

    assertEquals(80, context.getPlayerHealth());
    assertEquals(20, context.getEnemyHealth());
    assertEquals(30, context.getEnemyMaxHealth());
    assertEquals(7, context.getEnemyAttack());
    assertEquals(3, context.getEnemyArmour());
    assertSame(previousIntent, context.getPreviousIntent());
    assertEquals(2, context.getTurnNumber());
  }

  @Test
  void shouldCalculateEnemyHealthRatio() {
    EnemyAIContext context = createContext(100, 25, 100, 6, 0, 1);

    assertEquals(0.25f, context.getEnemyHealthRatio(), 0.001f);
  }

  @Test
  void shouldReturnZeroRatioWhenMaxHealthIsZero() {
    EnemyAIContext context = createContext(100, 10, 0, 6, 0, 1);

    assertEquals(0f, context.getEnemyHealthRatio());
  }

  @Test
  void shouldDetectWhenEnemyCanDefeatPlayer() {
    EnemyAIContext context = createContext(5, 20, 20, 6, 0, 1);

    assertTrue(context.canDefeatPlayer());
  }

  @Test
  void shouldDetectWhenEnemyCannotDefeatPlayer() {
    EnemyAIContext context = createContext(10, 20, 20, 6, 0, 1);

    assertFalse(context.canDefeatPlayer());
  }

  @Test
  void shouldDetectExactLethalDamage() {
    EnemyAIContext context = createContext(6, 20, 20, 6, 0, 1);

    assertTrue(context.canDefeatPlayer());
  }

  @Test
  void shouldClampNegativeBattleValuesToZero() {
    EnemyAIContext context = new EnemyAIContext(-10, -20, -30, -4, -5, EnemyIntent.unknown(), -2);

    assertEquals(0, context.getPlayerHealth());
    assertEquals(0, context.getEnemyHealth());
    assertEquals(0, context.getEnemyMaxHealth());
    assertEquals(0, context.getEnemyAttack());
    assertEquals(0, context.getEnemyArmour());
  }

  @Test
  void shouldClampTurnNumberToOne() {
    EnemyAIContext context = createContext(100, 20, 20, 6, 0, 0);

    assertEquals(1, context.getTurnNumber());
  }

  @Test
  void shouldRejectNullPreviousIntent() {
    assertThrows(NullPointerException.class, () -> new EnemyAIContext(100, 20, 20, 6, 0, null, 1));
  }

  private EnemyAIContext createContext(
      int playerHealth,
      int enemyHealth,
      int enemyMaxHealth,
      int enemyAttack,
      int enemyArmour,
      int turnNumber) {
    return new EnemyAIContext(
        playerHealth,
        enemyHealth,
        enemyMaxHealth,
        enemyAttack,
        enemyArmour,
        EnemyIntent.unknown(),
        turnNumber);
  }
}
