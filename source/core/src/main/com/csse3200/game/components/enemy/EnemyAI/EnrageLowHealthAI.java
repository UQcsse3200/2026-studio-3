package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import java.util.Objects;

/**
 * Elite behaviour that follows a four-stance combat pattern and may enter enrage based on either
 * combat health or observed player behaviour.
 *
 * <p>The base behaviour is provided by {@link CycleFourStanceAI}. Attack intents deal double damage
 * when either:
 *
 * <ul>
 *   <li>the enemy is below half health, or
 *   <li>player memory reports that the player has remained undefended.
 * </ul>
 *
 * <p>Defensive intents are never converted into attacks and are not otherwise changed by enrage.
 */
public class EnrageLowHealthAI implements EnemyAI {
  private static final float ENRAGE_THRESHOLD = 0.5f;
  private static final int ENRAGE_MULTIPLIER = 2;

  private final EnemyAI basePattern = new CycleFourStanceAI();

  @Override
  public EnemyIntent decide(EnemyAIContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    EnemyIntent baseIntent = basePattern.decide(context);

    boolean lowHealthEnrage = context.getEnemyHealthRatio() < ENRAGE_THRESHOLD;

    boolean memoryEnrage = context.getPlayerMemory().isUndefended();

    boolean isEnraged = lowHealthEnrage || memoryEnrage;

    if (isEnraged && baseIntent.getType() == IntentType.ATTACK) {
      return EnemyIntent.attack(baseIntent.getValue() * ENRAGE_MULTIPLIER);
    }

    return baseIntent;
  }
}
