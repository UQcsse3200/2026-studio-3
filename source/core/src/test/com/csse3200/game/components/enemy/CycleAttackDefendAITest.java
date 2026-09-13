package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CycleAttackDefendAITest {

  private EnemyStatsComponent newStats() {
    Entity entity = new Entity();
    EnemyStatsComponent stats = new EnemyStatsComponent(20, 6, 0);
    entity.addComponent(stats);
    entity.create();
    return stats;
  }

  // 验证攻/防交替：偶数回合攻击，奇数回合防御
  @Test
  void shouldAlternateAttackAndDefend() {
    CycleAttackDefendAI ai = new CycleAttackDefendAI();
    EnemyStatsComponent stats = newStats();

    assertEquals(IntentType.ATTACK, ai.decideIntent(stats).getType());
    assertEquals(IntentType.DEFEND, ai.decideIntent(stats).getType());
    assertEquals(IntentType.ATTACK, ai.decideIntent(stats).getType());
    assertEquals(IntentType.DEFEND, ai.decideIntent(stats).getType());
  }

  @Test
  void attackIntentUsesOwnBaseAttack() {
    CycleAttackDefendAI ai = new CycleAttackDefendAI();
    EnemyStatsComponent stats = newStats();

    assertEquals(6, ai.decideIntent(stats).getValue());
  }
}
