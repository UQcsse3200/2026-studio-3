package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnemyIntentDisplayTest {
    @Test
    void shouldDisplayPositiveDuration() {
        EnemyIntent intent =
                EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 2);

        assertEquals("2", EnemyIntentDisplay.durationTextFor(intent));
    }

    @Test
    void shouldDisplayMultiDigitDuration() {
        EnemyIntent intent =
                EnemyIntent.debuff(IntentEffectType.DAMAGE_ON_CARD_PLAY, 1, 12);

        assertEquals("12", EnemyIntentDisplay.durationTextFor(intent));
    }

    @Test
    void shouldNotDisplayZeroDuration() {
        EnemyIntent intent =
                EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 0);

        assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
    }

    @Test
    void shouldNotDisplayNegativeDuration() {
        EnemyIntent intent =
                EnemyIntent.debuff(IntentEffectType.SILENCE, 1, -1);

        assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
    }

    @Test
    void shouldNotDisplayDurationForNullIntent() {
        assertEquals("", EnemyIntentDisplay.durationTextFor(null));
    }

    @Test
    void shouldNotDisplayDurationForNormalAttack() {
        EnemyIntent intent = EnemyIntent.attack(6);

        assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
    }
}
