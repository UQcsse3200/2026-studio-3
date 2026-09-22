package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class CycleFourStanceAITest {
  private final CycleFourStanceAI ai = new CycleFourStanceAI();

  @Test
  void shouldAttackOnFirstTurn() {
    EnemyIntent intent = ai.decide(createContext(1, 0));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(7, intent.getValue());
  }

  @Test
  void shouldDefendOnSecondTurn() {
    EnemyIntent intent = ai.decide(createContext(2, 0));

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(4, intent.getValue());
  }

  @Test
  void shouldAttackOnThirdTurn() {
    EnemyIntent intent = ai.decide(createContext(3, 4));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(7, intent.getValue());
  }

  @Test
  void shouldAttackOnFourthTurnWhenArmourRemains() {
    EnemyIntent intent = ai.decide(createContext(4, 2));

    assertEquals(IntentType.ATTACK, intent.getType());
  }

  @Test
  void shouldDefendOnFourthTurnWhenArmourIsBroken() {
    EnemyIntent intent = ai.decide(createContext(4, 0));

    assertEquals(IntentType.DEFEND, intent.getType());
    assertEquals(4, intent.getValue());
  }

  @Test
  void shouldRestartCycleOnFifthTurn() {
    EnemyIntent intent = ai.decide(createContext(5, 0));

    assertEquals(IntentType.ATTACK, intent.getType());
  }

  private EnemyAIContext createContext(int turnNumber, int armour) {
    return new EnemyAIContext(100, 30, 30, 7, armour, EnemyIntent.unknown(), turnNumber);
  }
}
