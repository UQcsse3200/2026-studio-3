package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class CycleAttackDefendAITest {
  private final CycleAttackDefendAI ai = new CycleAttackDefendAI();

  @Test
  void shouldAttackOnOddTurn() {
    EnemyAIContext context = createContext(1);

    EnemyIntent intent = ai.decide(context);

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(6, intent.getValue());
  }

  @Test
  void shouldDefendOnEvenTurn() {
    EnemyAIContext context = createContext(2);

    EnemyIntent intent = ai.decide(context);

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(2, intent.getValue());
  }

  private EnemyAIContext createContext(int turnNumber) {
    return new EnemyAIContext(
        100, // playerHealth
        24, // enemyHealth
        24, // enemyMaxHealth
        6, // enemyAttack
        0, // enemyArmour
        EnemyIntent.unknown(), // previousIntent
        turnNumber);
  }
}
