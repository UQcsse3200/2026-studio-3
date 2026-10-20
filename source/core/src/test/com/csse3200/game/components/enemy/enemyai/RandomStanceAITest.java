package com.csse3200.game.components.enemy.enemyai;

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

  // A stubbed Random always returns a small value, inside the attack probability range
  @Test
  void shouldAttackWhenRollIsBelowAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.1);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.ATTACK, ai.decide(context()).getType());
  }

  // A stubbed Random always returns a large value, inside the defend probability range
  @Test
  void shouldDefendWhenRollIsAboveAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.9);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.DEFEND, ai.decide(context()).getType());
  }
}
