package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentEffectType;
import java.util.Objects;

/**
 * Elite behaviour built around shield breaking and retaliation.
 *
 * <p>While armour remains, the enemy attacks normally. The turn its armour is depleted it performs
 * an empowered retaliation, restores its armour on the following turn, then returns to attacking.
 * While its armour remains, it also periodically taunts the player. Shield-break retaliation and
 * restoration take priority over taunting.
 */
public class ShieldBreakRetaliationAI implements EnemyAI {
  private static final int RESTORED_ARMOUR = 5;
  private static final int RETALIATION_MULTIPLIER = 2;
  private static final int TAUNT_DURATION = 2;
  private static final int NORMAL_ATTACKS_BETWEEN_TAUNTS = 2;

  private boolean restoreShieldNextTurn;
  private int normalAttacksBeforeTaunt = NORMAL_ATTACKS_BETWEEN_TAUNTS;

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

    if (normalAttacksBeforeTaunt == 0) {
      normalAttacksBeforeTaunt = NORMAL_ATTACKS_BETWEEN_TAUNTS;
      return EnemyIntent.debuff(IntentEffectType.TAUNT, 0, TAUNT_DURATION);
    }

    normalAttacksBeforeTaunt--;
    return EnemyIntent.attack(context.getEnemyAttack());
  }
}
