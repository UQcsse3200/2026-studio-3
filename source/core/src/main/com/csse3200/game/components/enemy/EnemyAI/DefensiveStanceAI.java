package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Defensive-leaning behaviour for Bone Crawler: a fixed 4-turn cycle of defend, attack, defend,
 * defend (3 defends per attack).
 *
 * <p>骨爬虫（Bone Crawler）的打法：4 回合一个循环，防、攻、防、防——大部分时间在防御，偶尔才攻击一次。
 */
public class DefensiveStanceAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 5;
  private static final int CYCLE_LENGTH = 4;
  private static final int ATTACK_TURN = 2;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    int cycleTurn = ((context.getTurnNumber() - 1) % CYCLE_LENGTH) + 1;

    return (cycleTurn == ATTACK_TURN)
        ? EnemyIntent.attack(context.getEnemyAttack())
        : EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
