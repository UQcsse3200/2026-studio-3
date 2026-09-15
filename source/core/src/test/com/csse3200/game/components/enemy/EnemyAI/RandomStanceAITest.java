package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomStanceAITest {
  private EnemyAIContext context() {
    return new EnemyAIContext(30, 24, 24, 6, 0, EnemyIntent.unknown(), 1);
  }

  // 用打桩的 Random 固定返回一个小值，落在攻击的概率区间内
  @Test
  void shouldAttackWhenRollIsBelowAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.1);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.ATTACK, ai.decide(context()).getType());
  }

  // 用打桩的 Random 固定返回一个大值，落在防御的概率区间内
  @Test
  void shouldDefendWhenRollIsAboveAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.9);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.DEFEND, ai.decide(context()).getType());
  }
}
