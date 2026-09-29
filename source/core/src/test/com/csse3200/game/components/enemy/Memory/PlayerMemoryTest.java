package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerMemoryTest {

  @Test
  void shouldStorePlayerBehaviourValues() {
    PlayerMemory memory = new PlayerMemory(5, 2, 3, 4);

    assertAll(
        () -> assertEquals(5, memory.attackCardsPlayed()),
        () -> assertEquals(2, memory.skillCardsPlayed()),
        () -> assertEquals(3, memory.consecutiveTurnsWithoutBlock()),
        () -> assertEquals(4, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldFavourAttackWhenThresholdAndRatioAreMet() {
    PlayerMemory memory = new PlayerMemory(4, 1, 0, 0);

    assertTrue(memory.favoursAttack());
  }

  @Test
  void shouldNotFavourAttackBelowMinimumAttackThreshold() {
    PlayerMemory memory = new PlayerMemory(3, 0, 0, 0);

    assertFalse(memory.favoursAttack());
  }

  @Test
  void shouldNotFavourAttackWhenAttackCountIsExactlyTwiceSkillCount() {
    PlayerMemory memory = new PlayerMemory(4, 2, 0, 0);

    assertFalse(memory.favoursAttack());
  }

  @Test
  void shouldFavourAttackWhenAttackCountIsMoreThanTwiceSkillCount() {
    PlayerMemory memory = new PlayerMemory(5, 2, 0, 0);

    assertTrue(memory.favoursAttack());
  }

  @Test
  void shouldBeUndefendedAfterThreeTurnsWithoutBlock() {
    PlayerMemory memory = new PlayerMemory(0, 0, 3, 0);

    assertTrue(memory.isUndefended());
  }

  @Test
  void shouldNotBeUndefendedBeforeThreeTurnsWithoutBlock() {
    PlayerMemory memory = new PlayerMemory(0, 0, 2, 0);

    assertFalse(memory.isUndefended());
  }

  @Test
  void shouldRemainUndefendedBeyondThreshold() {
    PlayerMemory memory = new PlayerMemory(0, 0, 5, 0);

    assertTrue(memory.isUndefended());
  }

  @Test
  void shouldProvideNeutralEmptyMemory() {
    PlayerMemory memory = PlayerMemory.empty();

    assertAll(
        () -> assertEquals(0, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.skillCardsPlayed()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()),
        () -> assertEquals(0, memory.cardsPlayedLastTurn()),
        () -> assertFalse(memory.favoursAttack()),
        () -> assertFalse(memory.isUndefended()));
  }

  @Test
  void shouldReuseEmptyMemoryInstance() {
    assertSame(PlayerMemory.empty(), PlayerMemory.empty());
  }

  @Test
  void shouldRejectNegativeValues() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new PlayerMemory(-1, 0, 0, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new PlayerMemory(0, -1, 0, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new PlayerMemory(0, 0, -1, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new PlayerMemory(0, 0, 0, -1)));
  }

  @Test
  void shouldUseRecordValueEquality() {
    PlayerMemory first = new PlayerMemory(5, 2, 3, 1);
    PlayerMemory second = new PlayerMemory(5, 2, 3, 1);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }
}
