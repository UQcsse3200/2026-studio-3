package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Armor-scaling behaviour for Dark Acolyte: banks armor on odd turns and attacks on even turns for
 * base damage plus its currently-held armor.
 *
 * <p>The armor is read as a damage bonus and is not spent, so leaving the acolyte alone lets both
 * its armor and its damage climb every cycle. Breaking through the armor early is the intended
 * counterplay.
 */
public class ArmorScalingAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 4;
  private static final int CYCLE_LENGTH = 2;
  private static final int ATTACK_TURN = 2;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    int cycleTurn = ((context.getTurnNumber() - 1) % CYCLE_LENGTH) + 1;

    if (cycleTurn == ATTACK_TURN) {
      return EnemyIntent.attack(context.getEnemyAttack() + context.getEnemyArmor());
    }

    return EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
