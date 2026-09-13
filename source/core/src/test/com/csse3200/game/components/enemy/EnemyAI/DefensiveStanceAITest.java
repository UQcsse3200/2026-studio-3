package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class DefensiveStanceAITest {
  private final DefensiveStanceAI ai = new DefensiveStanceAI();

  // 骨爬虫的 4 拍循环应该是：防、攻、防、防，并且能循环第二遍
  @Test
  void shouldFollowDefendHeavyFourTurnCycle() {
    IntentType[] expected = {
      IntentType.DEFEND,
      IntentType.ATTACK,
      IntentType.DEFEND,
      IntentType.DEFEND,
      IntentType.DEFEND,
      IntentType.ATTACK,
      IntentType.DEFEND,
      IntentType.DEFEND
    };

    for (int turn = 1; turn <= expected.length; turn++) {
      EnemyIntent intent = ai.decide(createContext(turn));
      assertEquals(expected[turn - 1], intent.getType(), "turn " + turn);
    }
  }

  @Test
  void attackIntentUsesEnemyAttack() {
    EnemyIntent intent = ai.decide(createContext(2));

    assertEquals(5, intent.getValue());
  }

  private EnemyAIContext createContext(int turnNumber) {
    return new EnemyAIContext(30, 30, 30, 5, 0, EnemyIntent.unknown(), turnNumber);
  }
}
