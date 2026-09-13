package com.csse3200.game.components.enemy;

/**
 * "Trade armour for damage" behaviour for Dark Acolyte: alternates defending (gaining armour) and
 * attacking (spending all currently-held armour as bonus damage).
 *
 * <p>黑暗信徒（Dark Acolyte）的打法：先防御攒护甲，下一回合攻击时把当前护甲清零，
 * 全部转化成额外伤害——"用防御换输出"。护甲是在决定意图（rollIntent）时就被消耗掉的，
 * 而不是等到意图真正执行时，这一点如果以后接入回合系统需要注意。
 */
public class ArmourSacrificeAI implements EnemyAI {
  private static final int DEFEND_ARMOUR = 4;

  private int step;

  @Override
  public EnemyIntent decideIntent(EnemyStatsComponent self) {
    boolean isDefendTurn = step % 2 == 0;
    step++;

    if (isDefendTurn) {
      return EnemyIntent.defend(DEFEND_ARMOUR);
    }

    int bonusDamage = self.getArmour();
    self.setArmour(0);
    return EnemyIntent.attack(self.getBaseAttack() + bonusDamage);
  }
}
