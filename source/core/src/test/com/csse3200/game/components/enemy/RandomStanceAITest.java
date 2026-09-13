package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RandomStanceAITest {

  private EnemyStatsComponent newStats() {
    Entity entity = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(24, 6, 0);
    entity.addComponent(stats);
    entity.create();
    return stats;
  }

  // 用打桩的 Random 固定返回一个小值，落在攻击的概率区间内
  @Test
  void shouldAttackWhenRollIsBelowAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.1);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.ATTACK, ai.decideIntent(newStats()).getType());
  }

  // 用打桩的 Random 固定返回一个大值，落在防御的概率区间内
  @Test
  void shouldDefendWhenRollIsAboveAttackChance() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.9);
    RandomStanceAI ai = new RandomStanceAI(random);

    assertEquals(IntentType.DEFEND, ai.decideIntent(newStats()).getType());
  }
}
