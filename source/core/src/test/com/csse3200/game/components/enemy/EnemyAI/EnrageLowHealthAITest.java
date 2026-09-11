package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import org.junit.jupiter.api.Test;

class EnrageLowHealthAITest {
    private static final int BASE_ATTACK = 16;
    private final EnrageLowHealthAI ai = new EnrageLowHealthAI();

    @Test
    void shouldDealNormalDamageAboveHalfHealth() {
        EnemyIntent intent = ai.decide(
                createContext(30, 40, 0, 1));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BASE_ATTACK, intent.getValue());
    }

    @Test
    void shouldNotEnrageAtExactlyHalfHealth() {
        EnemyIntent intent = ai.decide(
                createContext(20, 40, 0, 1));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BASE_ATTACK, intent.getValue());
    }

    @Test
    void shouldDealDoubleDamageBelowHalfHealth() {
        EnemyIntent intent = ai.decide(
                createContext(19, 40, 0, 1));

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
