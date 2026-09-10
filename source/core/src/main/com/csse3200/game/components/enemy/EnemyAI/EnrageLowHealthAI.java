package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import java.util.Objects;

/**
 * Elite behaviour that fights harder as its own health falls.
 *
 * <p>It defends on even turns and attacks on odd turns. Once below half health it becomes enraged,
 * doubling the damage of every attack for the rest of the fight.
 */
public class EnrageLowHealthAI implements EnemyAI {
  private static final float ENRAGE_THRESHOLD = 0.5f;
  private static final int ENRAGE_MULTIPLIER = 2;

  private final EnemyAI basePattern = new CycleFourStanceAI();

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    EnemyIntent baseIntent = basePattern.decide(context);

    if (context.getEnemyHealthRatio() < ENRAGE_THRESHOLD
        && baseIntent.getType() == IntentType.ATTACK) {
      return EnemyIntent.attack(baseIntent.getValue() * ENRAGE_MULTIPLIER);
    }

    return baseIntent;
  }
}
