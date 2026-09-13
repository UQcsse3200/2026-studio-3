package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentType;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BossAITest {
    private static final int PLAYER_HEALTH = 100;
    private static final int BOSS_MAX_HEALTH = 100;
    private static final int BOSS_ATTACK = 10;

    @Test
    void shouldStartInPhaseOne() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(100, 0, 1));

        assertEquals(BossAI.BossPhase.PHASE_ONE, ai.getCurrentPhase());
    }

    @Test
    void shouldEnterPhaseTwoAtSixtyFivePercentHealth() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(65, 0, 1));

        assertEquals(BossAI.BossPhase.PHASE_TWO, ai.getCurrentPhase());
    }

    @Test
    void shouldEnterEnragedPhaseAtThirtyPercentHealth() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(30, 0, 1));

        assertEquals(BossAI.BossPhase.ENRAGED, ai.getCurrentPhase());
    }

    @Test
    void shouldNotReturnToEarlierPhaseAfterHealing() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(30, 0, 1));
        ai.decide(createContext(100, 0, 2));

        assertEquals(BossAI.BossPhase.ENRAGED, ai.getCurrentPhase());
    }

    @Test
    void shouldProduceAttackIntent() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        EnemyIntent intent = ai.decide(createContext(100, 0, 1));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BOSS_ATTACK, intent.getValue());
    }

    @Test
    void shouldProducePhaseOneDefendIntent() {
        BossAI ai = new BossAI(new FixedRollRandom(99));

        EnemyIntent intent = ai.decide(createContext(100, 0, 1));

        assertEquals(IntentType.DEFEND, intent.getType());
        assertEquals(4, intent.getValue());
    }

    @Test
    void shouldUseStrongerDefenceInPhaseTwo() {
        BossAI ai = new BossAI(new FixedRollRandom(99));

        EnemyIntent intent = ai.decide(createContext(60, 0, 1));

        assertEquals(IntentType.DEFEND, intent.getType());
        assertEquals(6, intent.getValue());
    }

    @Test
    void shouldAttackWhenArmorIsAlreadyHigh() {
        BossAI ai = new BossAI(new FixedRollRandom(99));

        EnemyIntent intent = ai.decide(createContext(100, 8, 1));

        assertEquals(IntentType.ATTACK, intent.getType());
        assertEquals(BOSS_ATTACK, intent.getValue());
    }

    @Test
    void shouldRecordPreviousMove() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(100, 0, 1));

        assertEquals(BossAI.BossMove.ATTACK, ai.getPreviousMove());
    }

    @Test
    void shouldCountConsecutiveAttacks() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        ai.decide(createContext(100, 0, 1));
        ai.decide(createContext(100, 0, 2));

        assertEquals(2, ai.getConsecutiveAttacks());
    }

    @Test
    void shouldResetConsecutiveAttacksAfterDefending() {
        BossAI ai = new BossAI(new SequenceRandom(0, 0, 99));

        ai.decide(createContext(100, 0, 1));
        ai.decide(createContext(100, 0, 2));
        ai.decide(createContext(100, 0, 3));

        assertEquals(BossAI.BossMove.DEFEND, ai.getPreviousMove());
        assertEquals(0, ai.getConsecutiveAttacks());
    }

    @Test
    void shouldRejectNullContext() {
        BossAI ai = new BossAI(new FixedRollRandom(0));

        assertThrows(NullPointerException.class, () -> ai.decide(null));
    }

    private EnemyAIContext createContext(
            int bossHealth, int bossArmor, int turnNumber) {
        return new EnemyAIContext(
                PLAYER_HEALTH,
                bossHealth,
                BOSS_MAX_HEALTH,
                BOSS_ATTACK,
                bossArmor,
                EnemyIntent.unknown(),
                turnNumber);
    }

    /**
     * Always returns the same configured roll.
     *
     * <p>A value of zero selects the beginning of the weighted table. A sufficiently large value
     * selects an action near the end of the table.
     */
    private static class FixedRollRandom extends Random {
        private static final long serialVersionUID = 1L;

        private final int fixedRoll;

        FixedRollRandom(int fixedRoll) {
            this.fixedRoll = fixedRoll;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(fixedRoll, bound - 1);
        }
    }

    /**
     * Returns a configured sequence of rolls.
     *
     * <p>After every configured roll has been used, the final configured value is reused.
     */
    private static class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;

        private final int[] rolls;
        private int index;

        SequenceRandom(int... rolls) {
            this.rolls = rolls;
        }

        @Override
        public int nextInt(int bound) {
            int rollIndex = Math.min(index, rolls.length - 1);
            int configuredRoll = rolls[rollIndex];
            index++;

            return Math.min(configuredRoll, bound - 1);
        }
    }
}