package com.csse3200.game.components.enemy;

/**
 * Defensive-leaning behaviour for Bone Crawler: a fixed 4-round cycle of defend, attack, defend,
 * defend (3 defends per attack).
 *
 * <p>骨爬虫（Bone Crawler）的打法：4 回合一个循环，防、攻、防、防——大部分时间在防御， 偶尔才攻击一次，体现"偏保守"的风格。
 */
public class CycleFourStanceAI implements EnemyAI {
  private static final int DEFEND_ARMOUR = 5;
  private static final IntentType[] CYCLE = {
    IntentType.DEFEND, IntentType.ATTACK, IntentType.DEFEND, IntentType.DEFEND
  };

  private int step;

  @Override
  public EnemyIntent decideIntent(EnemyStatsComponent self) {
    IntentType type = CYCLE[step % CYCLE.length];
    step++;

    return (type == IntentType.ATTACK)
        ? EnemyIntent.attack(self.getBaseAttack())
        : EnemyIntent.defend(DEFEND_ARMOUR);
  }
}
