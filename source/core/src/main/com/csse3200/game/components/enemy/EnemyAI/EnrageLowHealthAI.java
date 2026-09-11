package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import java.util.Objects;

/**
 * Elite behaviour that follows a four-stance combat pattern and becomes
 * stronger at low health.
 *
 * <p>The base behaviour is provided by {@link CycleFourStanceAI}. When the
 * enemy falls below half health, all attack intents deal double damage.
 * Defensive intents are not affected by enrage.
 */
public class EnrageLowHealthAI implements EnemyAI {
  private static final float ENRAGE_THRESHOLD = 0.5f;
  private static final int ENRAGE_MULTIPLIER = 2;

  private final EnemyAI basePattern = new CycleFourStanceAI();

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    EnemyIntent baseIntent = basePattern.decide(context);

    boolean isEnraged =
            context.getEnemyHealthRatio() < ENRAGE_THRESHOLD;

    if (isEnraged && baseIntent.getType() == IntentType.ATTACK) {
      return EnemyIntent.attack(
              baseIntent.getValue() * ENRAGE_MULTIPLIER);
    }

    return baseIntent;
  }
}
