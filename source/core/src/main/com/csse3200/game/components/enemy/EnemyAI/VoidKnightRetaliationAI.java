package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Void Knight elite behaviour based on shield breaking and retaliation.
 *
 * <p>While armour remains, the knight attacks normally. When its armour is depleted, it performs an
 * empowered retaliation, restores its armour on the following turn, and then returns to its normal
 * attacking state.
 */
public class VoidKnightRetaliationAI implements EnemyAI {
  private static final int RESTORED_ARMOUR = 5;
  private static final int RETALIATION_MULTIPLIER = 2;

  private boolean restoreShieldNextTurn;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    if (restoreShieldNextTurn) {
      restoreShieldNextTurn = false;
      return EnemyIntent.defend(RESTORED_ARMOUR);
    }

    if (context.getEnemyArmor() == 0) {
      restoreShieldNextTurn = true;
      return EnemyIntent.attack(context.getEnemyAttack() * RETALIATION_MULTIPLIER);
    }

    return EnemyIntent.attack(context.getEnemyAttack());
  }
}
