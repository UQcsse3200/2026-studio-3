package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class VoidKnightRetaliationAITest {
    private static final int BASE_ATTACK = 10;
    private static final int RESTORED_ARMOUR = 5;

    @Test
    void shouldAttackNormallyWhileArmourRemains() {
        VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

        EnemyIntent intent =
                ai.decide(createContext(72, 72, 5, 1));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BASE_ATTACK, intent.getValue());
    }

    @Test
    void shouldRetaliateWhenArmourIsBroken() {
        VoidKnightRetaliationAI ai = new VoidKnightRetaliationAI();

        EnemyIntent intent =
                ai.decide(createContext(72, 72, 0, 2));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BASE_ATTACK * 2, intent.getValue());
    }

    private EnemyAIContext createContext(
            int health,
            int maxHealth,
            int armour,
            int turnNumber) {
        return new EnemyAIContext(
                100,
                health,
                maxHealth,
                BASE_ATTACK,
                armour,
                EnemyIntent.unknown(),
                turnNumber);
    }
}