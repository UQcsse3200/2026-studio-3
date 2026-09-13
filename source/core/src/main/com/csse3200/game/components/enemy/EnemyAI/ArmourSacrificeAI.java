package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * "Trade armour for damage" behaviour for Dark Acolyte: alternates defending (banking armour) and
 * attacking with a bonus equal to its currently-held armour.
 *
 * <p>黑暗信徒（Dark Acolyte）的打法：先防御攒护甲，下一回合攻击时把当前护甲值加到伤害上—— "用防御换输出"。护甲本身不会被清零，攻击伤害只是额外算上当前护甲值。
 */
public class ArmourSacrificeAI implements EnemyAI {
  private static final int DEFEND_AMOUNT = 4;

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    if (context.getTurnNumber() % 2 == 0) {
      return EnemyIntent.attack(context.getEnemyAttack() + context.getEnemyArmor());
    }

    return EnemyIntent.defend(DEFEND_AMOUNT);
  }
}
