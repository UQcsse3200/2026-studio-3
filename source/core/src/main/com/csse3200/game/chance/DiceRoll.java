package com.csse3200.game.chance;

/** Read-only result of one two-dice roll for the encounter display. */
public record DiceRoll(int firstDie, int secondDie, int sequence) {
  public DiceRoll {
    if (firstDie < 1 || firstDie > 6 || secondDie < 1 || secondDie > 6) {
      throw new IllegalArgumentException("Each die must show a value from 1 to 6");
    }
    if (sequence < 1) {
      throw new IllegalArgumentException("Roll sequence must be positive");
    }
  }

  public int total() {
    return firstDie + secondDie;
  }
}
