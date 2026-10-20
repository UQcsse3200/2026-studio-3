package com.csse3200.game.components.enemy.enemyai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class TutorialEnemyAITest {
  private final TutorialEnemyAI ai = new TutorialEnemyAI();

  @Test
  void shouldAttackOnFirstTurn() {
    EnemyIntent intent = ai.decide(createContext(1, 20, 6));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(6, intent.getValue());
  }

  @Test
  void shouldDefendOnSecondTurn() {
    EnemyIntent intent = ai.decide(createContext(2, 20, 6));

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(5, intent.getValue());
  }

  @Test
  void shouldBuffOnThirdTurn() {
    EnemyIntent intent = ai.decide(createContext(3, 20, 6));

    assertEquals(IntentType.BUFF, intent.getType());
    assertEquals(2, intent.getValue());
  }

  @Test
  void shouldRepeatFixedCycle() {
    assertEquals(IntentType.ATTACK, decideType(1));
    assertEquals(IntentType.DEFEND, decideType(2));
    assertEquals(IntentType.BUFF, decideType(3));
    assertEquals(IntentType.ATTACK, decideType(4));
    assertEquals(IntentType.DEFEND, decideType(5));
    assertEquals(IntentType.BUFF, decideType(6));
  }

  @Test
  void shouldUseFullAttackWhenAttackIsNotLethal() {
    EnemyIntent intent = ai.decide(createContext(1, 20, 6));

    assertEquals(6, intent.getValue());
  }

  @Test
  void shouldReduceAttackToLeavePlayerAtOneHealth() {
    EnemyIntent intent = ai.decide(createContext(1, 4, 10));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(3, intent.getValue());
    assertTrue(intent.getValue() < 4);
  }

  @Test
  void shouldDealZeroDamageWhenPlayerHasOneHealth() {
    EnemyIntent intent = ai.decide(createContext(1, 1, 10));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(0, intent.getValue());
  }

  @Test
  void shouldDealZeroDamageWhenPlayerHealthIsUnknown() {
    EnemyIntent intent = ai.decide(createContext(1, 0, 10));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(0, intent.getValue());
  }

  @Test
  void shouldRejectNullContext() {
    assertThrows(NullPointerException.class, () -> ai.decide(null));
  }

  private IntentType decideType(int turnNumber) {
    return ai.decide(createContext(turnNumber, 20, 6)).getType();
  }

  private EnemyAIContext createContext(int turnNumber, int playerHealth, int enemyAttack) {
    return new EnemyAIContext(
        playerHealth, 20, 20, enemyAttack, 0, EnemyIntent.unknown(), turnNumber);
  }
}
