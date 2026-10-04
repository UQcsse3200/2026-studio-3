package com.csse3200.game.components.enemy.Memory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.CardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnemyMemoryComponentTest {
  private EnemyMemoryComponent memory;

  @BeforeEach
  void setUp() {
    memory = new EnemyMemoryComponent();
  }

  @Test
  void shouldStartWithEmptyMemory() {
    assertEquals(PlayerMemory.empty(), memory.snapshot());
  }

  @Test
  void shouldRejectNullCardType() {
    assertThrows(NullPointerException.class, () -> memory.recordCardPlayed(null, false));
  }

  @Test
  void shouldCountAttackAndSkillCards() {
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.SKILL, false);

    PlayerMemory snapshot = memory.snapshot();

    assertAll(
        () -> assertEquals(2, snapshot.attackCardsPlayed()),
        () -> assertEquals(1, snapshot.skillCardsPlayed()),
        () -> assertEquals(0, snapshot.cardsPlayedLastTurn()));
  }

  @Test
  void shouldCountOtherTypesAsCardsPlayedDuringTurn() {
    memory.recordCardPlayed(CardType.POWER, false);
    memory.recordCardPlayed(CardType.STATUS, false);
    memory.recordCardPlayed(CardType.CURSE, false);

    memory.settlePlayerTurn();

    PlayerMemory snapshot = memory.snapshot();

    assertAll(
        () -> assertEquals(0, snapshot.attackCardsPlayed()),
        () -> assertEquals(0, snapshot.skillCardsPlayed()),
        () -> assertEquals(3, snapshot.cardsPlayedLastTurn()));
  }

  @Test
  void shouldNotExposeCurrentTurnCountBeforeSettlement() {
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.SKILL, false);

    assertEquals(0, memory.snapshot().cardsPlayedLastTurn());

    memory.settlePlayerTurn();

    assertEquals(2, memory.snapshot().cardsPlayedLastTurn());
  }

  @Test
  void shouldReplaceLastTurnCardCount() {
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.settlePlayerTurn();

    assertEquals(1, memory.snapshot().cardsPlayedLastTurn());

    memory.recordCardPlayed(CardType.SKILL, false);
    memory.recordCardPlayed(CardType.POWER, false);
    memory.settlePlayerTurn();

    assertEquals(2, memory.snapshot().cardsPlayedLastTurn());
  }

  @Test
  void shouldIncreaseNoBlockStreak() {
    memory.settlePlayerTurn();
    memory.settlePlayerTurn();
    memory.settlePlayerTurn();

    assertEquals(3, memory.snapshot().consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldResetNoBlockStreakAfterBlock() {
    memory.settlePlayerTurn();
    memory.settlePlayerTurn();

    assertEquals(2, memory.snapshot().consecutiveTurnsWithoutBlock());

    memory.recordCardPlayed(CardType.SKILL, true);
    memory.settlePlayerTurn();

    assertEquals(0, memory.snapshot().consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldRememberBlockForEntireTurn() {
    memory.recordCardPlayed(CardType.SKILL, true);
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.SKILL, false);

    memory.settlePlayerTurn();

    PlayerMemory snapshot = memory.snapshot();

    assertAll(
        () -> assertEquals(1, snapshot.attackCardsPlayed()),
        () -> assertEquals(2, snapshot.skillCardsPlayed()),
        () -> assertEquals(3, snapshot.cardsPlayedLastTurn()),
        () -> assertEquals(0, snapshot.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldResetBlockFlagBetweenTurns() {
    memory.recordCardPlayed(CardType.SKILL, true);
    memory.settlePlayerTurn();

    assertEquals(0, memory.snapshot().consecutiveTurnsWithoutBlock());

    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.settlePlayerTurn();

    assertEquals(1, memory.snapshot().consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldKeepLifetimeTotalsAcrossTurns() {
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.SKILL, true);
    memory.settlePlayerTurn();

    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.recordCardPlayed(CardType.ATTACK, false);
    memory.settlePlayerTurn();

    PlayerMemory snapshot = memory.snapshot();

    assertAll(
        () -> assertEquals(3, snapshot.attackCardsPlayed()),
        () -> assertEquals(1, snapshot.skillCardsPlayed()),
        () -> assertEquals(2, snapshot.cardsPlayedLastTurn()),
        () -> assertEquals(1, snapshot.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldReturnIndependentSnapshots() {
    PlayerMemory beforeCard = memory.snapshot();

    memory.recordCardPlayed(CardType.ATTACK, false);

    PlayerMemory afterCard = memory.snapshot();

    assertAll(
        () -> assertNotSame(beforeCard, afterCard),
        () -> assertEquals(0, beforeCard.attackCardsPlayed()),
        () -> assertEquals(1, afterCard.attackCardsPlayed()));
  }
}
