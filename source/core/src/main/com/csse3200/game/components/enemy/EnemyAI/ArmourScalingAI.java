package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Armour-scaling behaviour for Dark Acolyte.
 *
 * <p>Normally, the acolyte alternates between defending and attacking. Its attack deals base damage
 * plus its currently held armour.
 *
 * <p>When player memory indicates that the player strongly favours attack cards, the acolyte adapts
 * by defending twice before attacking. This allows it to stack more armour against an aggressive
 * player while preserving its deterministic combat pattern.
 *
 * <p>Armour contributes to attack damage but is not consumed.
 */
public class ArmourScalingAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 4;

  private static final int NORMAL_CYCLE_LENGTH = 2;
  private static final int REACTIVE_CYCLE_LENGTH = 3;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    boolean playerFavoursAttack = context.getPlayerMemory().favoursAttack();

    int cycleLength = playerFavoursAttack ? REACTIVE_CYCLE_LENGTH : NORMAL_CYCLE_LENGTH;

    int cycleTurn = ((context.getTurnNumber() - 1) % cycleLength) + 1;

    if (cycleTurn == cycleLength) {
      return EnemyIntent.attack(context.getEnemyAttack() + context.getEnemyArmour());
    }

    return EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
