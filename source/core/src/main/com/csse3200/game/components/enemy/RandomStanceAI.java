package com.csse3200.game.components.enemy;

import java.util.Random;

/**
 * Unpredictable behaviour for Lesser Shade: each round randomly attacks or defends.
 *
 * <p>暗影小怪（Lesser Shade）的打法：每回合像抛硬币一样随机决定攻击还是防御， 让玩家猜不到下一步。{@link Random} 通过构造函数传入，方便单元测试用固定的随机源
 * 来复现确定的结果。
 */
public class RandomStanceAI implements EnemyAI {
  private static final double ATTACK_CHANCE = 0.6;
  private static final int DEFEND_ARMOUR = 3;

  private final Random random;

  public RandomStanceAI(Random random) {
    this.random = random;
  }

  @Override
  public EnemyIntent decideIntent(EnemyStatsComponent self) {
    return (random.nextDouble() < ATTACK_CHANCE)
        ? EnemyIntent.attack(self.getBaseAttack())
        : EnemyIntent.defend(DEFEND_ARMOUR);
  }
}
