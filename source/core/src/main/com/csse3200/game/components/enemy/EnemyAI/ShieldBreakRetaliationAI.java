package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Elite behaviour built around shield breaking and retaliation.
 *
 * <p>While armour remains, the enemy attacks normally. The turn its armour is depleted it performs
 * an empowered retaliation, restores its armour on the following turn, then returns to attacking.
 * This makes breaking through the shield a trap rather than a reward: the intended counterplay is
 * one burst large enough to punch past the armour, not chipping it down.
 */
public class ShieldBreakRetaliationAI implements EnemyAI {
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

    if (context.getEnemyArmour() == 0) {
      restoreShieldNextTurn = true;
      return EnemyIntent.attack(context.getEnemyAttack() * RETALIATION_MULTIPLIER);
    }

    return EnemyIntent.attack(context.getEnemyAttack());
  }
}
