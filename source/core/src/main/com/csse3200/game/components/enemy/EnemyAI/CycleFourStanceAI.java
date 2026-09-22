package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * A stance-based behaviour that alternates between pressure and defence.
 *
 * <p>It follows a four-turn cycle:
 *
 * <ol>
 *   <li>Attack
 *   <li>Defend
 *   <li>Attack
 *   <li>Attack if armour remains, otherwise defend
 * </ol>
 */
public class CycleFourStanceAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 4;
  private static final int CYCLE_LENGTH = 4;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    int cycleTurn = ((context.getTurnNumber() - 1) % CYCLE_LENGTH) + 1;

    return switch (cycleTurn) {
      case 1 -> attack(context);
      case 2 -> defend();
      case 3 -> attack(context);
      case 4 -> decideFinalStance(context);
      default -> throw new IllegalStateException("Unexpected cycle turn: " + cycleTurn);
    };
  }

  private EnemyIntent decideFinalStance(EnemyAIContext context) {
    if (context.getEnemyArmour() > 0) {
      return attack(context);
    }

    return defend();
  }

  private EnemyIntent attack(EnemyAIContext context) {
    return EnemyIntent.attack(context.getEnemyAttack());
  }

  private EnemyIntent defend() {
    return EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
