package com.csse3200.game.components.enemy.EnemyAI;

import com.csse3200.game.components.enemy.EnemyIntent;
import java.util.Objects;

/**
 * Elite behaviour that fights harder as its own health falls.
 *
 * <p>It defends on even turns and attacks on odd turns. Once below half health it becomes
 * enraged, doubling the damage of every attack for the rest of the fight.
 */
public class EnrageLowHealthAI implements EnemyAI {
    private static final int DEFEND_AMOUNT = 3;
    private static final float ENRAGE_THRESHOLD = 0.5f;
    private static final int ENRAGE_MULTIPLIER = 2;

    @Override
    public EnemyIntent decide(EnemyAIContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        if (context.getTurnNumber() % 2 == 0) {
            return EnemyIntent.defend(DEFEND_AMOUNT);
        }

        int damage = context.getEnemyAttack();
        if (context.getEnemyHealthRatio() < ENRAGE_THRESHOLD) {
            damage *= ENRAGE_MULTIPLIER;
        }
        return EnemyIntent.attack(damage);
    }
}
