package com.csse3200.game.components.enemy;

/**
 * Baseline behaviour: alternates attack and defend every round.
 *
 * <p>基线打法：攻、防交替。作为其他敌人尚未分配专属行为时的兜底实现，目前由 Void Knight 使用。
 */
public class CycleAttackDefendAI implements EnemyAI {
  private static final int DEFEND_ARMOUR = 4;

  private int step;

  @Override
  public EnemyIntent decideIntent(EnemyStatsComponent self) {
    EnemyIntent intent =
        (step % 2 == 0)
            ? EnemyIntent.attack(self.getBaseAttack())
            : EnemyIntent.defend(DEFEND_ARMOUR);
    step++;
    return intent;
  }
}
