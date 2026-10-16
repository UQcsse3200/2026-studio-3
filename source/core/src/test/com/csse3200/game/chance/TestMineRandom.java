package com.csse3200.game.chance;

import java.util.Random;

/** Controllable test-only Mine random source that counts probability and item draws. */
public final class TestMineRandom extends Random {
  private static final long serialVersionUID = 1L;
  private final double trigger;
  private final int item;
  public int chanceDraws;
  public int itemDraws;

  public TestMineRandom(double trigger, int item) {
    this.trigger = trigger;
    this.item = item;
  }

  @Override
  public double nextDouble() {
    chanceDraws++;
    return trigger;
  }

  @Override
  public int nextInt(int bound) {
    itemDraws++;
    if (item < 0 || item >= bound)
      throw new IllegalArgumentException("Test item roll outside bound");
    return item;
  }
}
