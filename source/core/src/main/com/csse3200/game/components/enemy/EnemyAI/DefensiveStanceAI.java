package com.csse3200.game.components.enemy.enemyai;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Defensive-leaning behaviour for Bone Crawler: a fixed 4-turn cycle of defend, attack, defend,
 * defend (3 defends per attack).
 *
 * <p>How the Bone Crawler fights: a 4-turn cycle of defend, attack, defend, defend. It spends most
 * of its time defending and only attacks now and then.
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
