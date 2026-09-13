package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CycleFourStanceAITest {

  private EnemyStatsComponent newStats() {
    Entity entity = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(30, 5, 0);
    entity.addComponent(stats);
    entity.create();
    return stats;
  }

  // 骨爬虫的 4 拍循环应该是：防、攻、防、防，并且能循环第二遍
  @Test
  void shouldFollowDefendHeavyFourStepCycle() {
    CycleFourStanceAI ai = new CycleFourStanceAI();
    EnemyStatsComponent stats = newStats();

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

    for (IntentType type : expected) {
      assertEquals(type, ai.decideIntent(stats).getType());
    }
  }
}
