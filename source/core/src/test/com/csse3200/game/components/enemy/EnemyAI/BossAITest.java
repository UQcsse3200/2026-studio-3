package com.csse3200.game.components.enemy.EnemyAI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentEffectType;
import com.csse3200.game.components.enemy.IntentType;
import com.csse3200.game.components.enemy.Memory.PlayerMemory;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BossAITest {
  private static final int PLAYER_HEALTH = 100;
  private static final int BOSS_MAX_HEALTH = 100;
  private static final int BOSS_ATTACK = 10;

  /**
   * Rolls that land on a debuff entry in the weighted table.
   *
   * <p>Attacks and defences are drawn first, so a debuff needs a roll past the end of their
   * combined weight. The exact values depend on the phase weight tables in {@link BossAI}.
   */
  private static final int SILENCE_ROLL = 105;

  private static final int CARD_PLAY_DAMAGE_ROLL = 120;

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
  void shouldAttackWhenArmourIsAlreadyHigh() {
    BossAI ai = new BossAI(new FixedRollRandom(99));

    EnemyIntent intent = ai.decide(createContext(100, 8, 1));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BOSS_ATTACK, intent.getValue());
  }

  @Test
  void shouldBecomeMoreAggressiveUnderHighPressure() {
    BossAI lowPressureAI = new BossAI(new FixedRollRandom(40));
    BossAI highPressureAI = new BossAI(new FixedRollRandom(40));

    EnemyIntent lowPressureIntent =
            lowPressureAI.decide(createContext(100, 100, 0, 1));

    EnemyIntent highPressureIntent =
            highPressureAI.decide(createContext(10, 70, 0, 5));

    assertEquals(IntentType.DEFEND, lowPressureIntent.getType());
    assertEquals(IntentType.ATTACK, highPressureIntent.getType());
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

  @Test
  void shouldNotDebuffInPhaseOneWithoutMemoryTrigger() {
    BossAI ai = new BossAI(new FixedRollRandom(Integer.MAX_VALUE));

    EnemyIntent intent = ai.decide(createContext(100, 0, 1));

    assertNotEquals(IntentType.DEBUFF, intent.getType());
    assertEquals(0, ai.getDebuffCooldownRemaining());
  }

  @Test
  void shouldSilenceThePlayerInPhaseTwo() {
    BossAI ai = new BossAI(new FixedRollRandom(SILENCE_ROLL));

    EnemyIntent intent = ai.decide(createContext(60, 0, 1));

    assertEquals(IntentType.DEBUFF, intent.getType());
    assertEquals(IntentEffectType.SILENCE, intent.getEffectType());
    assertEquals(1, intent.getValue());
    assertEquals(2, intent.getDuration());
  }

  @Test
  void shouldDamageThePlayerOnCardPlayWhenEnraged() {
    BossAI ai = new BossAI(new FixedRollRandom(CARD_PLAY_DAMAGE_ROLL));

    EnemyIntent intent = ai.decide(createContext(20, 0, 1));

    assertEquals(IntentType.DEBUFF, intent.getType());
    assertEquals(IntentEffectType.DAMAGE_ON_CARD_PLAY, intent.getEffectType());
    assertEquals(3, intent.getValue());
    assertEquals(3, intent.getDuration());
  }

  @Test
  void shouldNotDebuffTwiceInARow() {
    BossAI ai = new BossAI(new FixedRollRandom(SILENCE_ROLL));

    EnemyIntent first = ai.decide(createContext(60, 0, 1));
    EnemyIntent second = ai.decide(createContext(60, 0, 2));

    assertEquals(IntentType.DEBUFF, first.getType());
    assertNotEquals(IntentType.DEBUFF, second.getType());
  }

  @Test
  void shouldDebuffAgainOnceTheCooldownExpires() {
    BossAI ai = new BossAI(new FixedRollRandom(SILENCE_ROLL));

    ai.decide(createContext(60, 0, 1));
    ai.decide(createContext(60, 0, 2));
    ai.decide(createContext(60, 0, 3));
    EnemyIntent fourth = ai.decide(createContext(60, 0, 4));

    assertEquals(IntentType.DEBUFF, fourth.getType());
  }

  @Test
  void shouldResetConsecutiveAttacksAfterDebuffing() {
    BossAI ai = new BossAI(new SequenceRandom(0, SILENCE_ROLL));

    ai.decide(createContext(60, 0, 1));
    ai.decide(createContext(60, 0, 2));

    assertEquals(BossAI.BossMove.SILENCE, ai.getPreviousMove());
    assertEquals(0, ai.getConsecutiveAttacks());
  }

  @Test
  void shouldPrioritiseSilenceAfterFourCardsWerePlayed() {
    BossAI ai = new BossAI(new FixedRollRandom(0));
    PlayerMemory memory = new PlayerMemory(2, 2, 0, 4);

    EnemyIntent intent =
            ai.decide(createContext(100, 0, 1, memory));

    assertEquals(IntentType.DEBUFF, intent.getType());
    assertEquals(IntentEffectType.SILENCE, intent.getEffectType());
    assertEquals(1, intent.getValue());
    assertEquals(2, intent.getDuration());
    assertEquals(BossAI.BossMove.SILENCE, ai.getPreviousMove());
    assertEquals(3, ai.getDebuffCooldownRemaining());
  }

  @Test
  void shouldNotPrioritiseSilenceBelowCardThreshold() {
    BossAI ai = new BossAI(new FixedRollRandom(0));
    PlayerMemory memory = new PlayerMemory(2, 1, 0, 3);

    EnemyIntent intent =
            ai.decide(createContext(100, 0, 1, memory));

    assertEquals(IntentType.ATTACK, intent.getType());
    assertEquals(BossAI.BossMove.ATTACK, ai.getPreviousMove());
    assertEquals(0, ai.getDebuffCooldownRemaining());
  }

  @Test
  void shouldNotBypassDebuffCooldownForMemorySilence() {
    BossAI ai = new BossAI(new FixedRollRandom(0));
    PlayerMemory highCardMemory = new PlayerMemory(2, 2, 0, 4);

    EnemyIntent first =
            ai.decide(createContext(100, 0, 1, highCardMemory));

    EnemyIntent second =
            ai.decide(createContext(100, 0, 2, highCardMemory));

    assertEquals(IntentEffectType.SILENCE, first.getEffectType());
    assertNotEquals(IntentType.DEBUFF, second.getType());
    assertEquals(2, ai.getDebuffCooldownRemaining());
  }

  @Test
  void shouldPrioritiseMemorySilenceAfterCooldownExpires() {
    BossAI ai = new BossAI(new FixedRollRandom(0));
    PlayerMemory highCardMemory = new PlayerMemory(2, 2, 0, 4);

    ai.decide(createContext(100, 0, 1, highCardMemory));
    ai.decide(createContext(100, 0, 2, highCardMemory));
    ai.decide(createContext(100, 0, 3, highCardMemory));

    EnemyIntent fourth =
            ai.decide(createContext(100, 0, 4, highCardMemory));

    assertEquals(IntentType.DEBUFF, fourth.getType());
    assertEquals(IntentEffectType.SILENCE, fourth.getEffectType());
    assertEquals(BossAI.BossMove.SILENCE, ai.getPreviousMove());
    assertEquals(3, ai.getDebuffCooldownRemaining());
  }

  @Test
  void shouldShareCooldownBetweenCardPlayDamageAndMemorySilence() {
    BossAI ai = new BossAI(new FixedRollRandom(CARD_PLAY_DAMAGE_ROLL));
    PlayerMemory highCardMemory = new PlayerMemory(2, 2, 0, 4);

    EnemyIntent first =
            ai.decide(createContext(20, 0, 1, PlayerMemory.empty()));

    EnemyIntent second =
            ai.decide(createContext(20, 0, 2, highCardMemory));

    assertEquals(IntentType.DEBUFF, first.getType());
    assertEquals(
            IntentEffectType.DAMAGE_ON_CARD_PLAY,
            first.getEffectType());

    assertNotEquals(IntentType.DEBUFF, second.getType());
    assertEquals(2, ai.getDebuffCooldownRemaining());
  }

  private EnemyAIContext createContext(
          int bossHealth, int bossArmour, int turnNumber) {
    return createContext(
            PLAYER_HEALTH,
            bossHealth,
            bossArmour,
            turnNumber,
            PlayerMemory.empty());
  }

  private EnemyAIContext createContext(
          int bossHealth,
          int bossArmour,
          int turnNumber,
          PlayerMemory playerMemory) {
    return createContext(
            PLAYER_HEALTH,
            bossHealth,
            bossArmour,
            turnNumber,
            playerMemory);
  }

  private EnemyAIContext createContext(
          int playerHealth,
          int bossHealth,
          int bossArmour,
          int turnNumber) {
    return createContext(
            playerHealth,
            bossHealth,
            bossArmour,
            turnNumber,
            PlayerMemory.empty());
  }

  private EnemyAIContext createContext(
          int playerHealth,
          int bossHealth,
          int bossArmour,
          int turnNumber,
          PlayerMemory playerMemory) {
    return new EnemyAIContext(
            playerHealth,
            bossHealth,
            BOSS_MAX_HEALTH,
            BOSS_ATTACK,
            bossArmour,
            EnemyIntent.unknown(),
            turnNumber,
            playerMemory);
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