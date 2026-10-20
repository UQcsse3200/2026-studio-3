package com.csse3200.game.components.enemy.enemyai;

import java.util.Objects;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;

/**
 * Tutorial enemy AI that follows a fixed attack, defend and buff cycle.
 *
 * <p>Attack damage is limited so that a normal tutorial attack cannot reduce a living player below
 * one health.
 */
public class TutorialEnemyAI implements EnemyAI {
  private static final int CYCLE_LENGTH = 3;
  private static final int ATTACK_STEP = 0;
  private static final int DEFEND_STEP = 1;

  private static final int DEFEND_AMOUNT = 5;
  private static final int BUFF_AMOUNT = 2;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    int cycleStep = (context.getTurnNumber() - 1) % CYCLE_LENGTH;

    return switch (cycleStep) {
      case ATTACK_STEP -> createNonLethalAttack(context);
      case DEFEND_STEP -> EnemyIntent.defend(DEFEND_AMOUNT);
      default -> new EnemyIntent(IntentType.BUFF, BUFF_AMOUNT);
    };
  }

  /**
   * Creates an attack that leaves a living player with at least one health.
   *
   * @param context current battle information
   * @return a non-lethal attack intent
   */
  private EnemyIntent createNonLethalAttack(EnemyAIContext context) {
    int maximumSafeDamage = Math.max(0, context.getPlayerHealth() - 1);
    int damage = Math.min(context.getEnemyAttack(), maximumSafeDamage);

    return EnemyIntent.attack(damage);
  }
}
