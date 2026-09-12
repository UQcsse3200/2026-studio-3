package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/** Basic enemy AI that alternates between attacking and defending. */
public class CycleAttackDefendAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 2;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    if (context.getTurnNumber() % 2 == 0) {
      return EnemyIntent.defend(DEFEND_AMOUNT);
    }

    return EnemyIntent.attack(context.getEnemyAttack());
  }
}
