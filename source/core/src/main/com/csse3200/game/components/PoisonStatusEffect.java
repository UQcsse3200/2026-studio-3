package com.csse3200.game.components;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Internal poison storage, grouped by remaining ticks. Key 0 represents permanent poison.
 * Applications with equal remaining durations add stacks without extending their duration.
 */
final class PoisonStatusEffect extends StatusEffect {
  private final Map<Integer, Integer> stacksByDuration = new HashMap<>();
  private final Map<Integer, Integer> pendingApplications = new HashMap<>();
  private boolean processing;

  PoisonStatusEffect() {
    super("POISON", 0, 0);
  }

  void addApplication(int stacks, int duration) {
    // New poison applied by a damage listener starts ticking on the next poison tick.
    merge(processing ? pendingApplications : stacksByDuration, duration, stacks);
  }

  private static void merge(Map<Integer, Integer> target, int duration, int stacks) {
    target.merge(Math.max(0, duration), Math.max(0, stacks), PoisonStatusEffect::sum);
  }

  private static int sum(int left, int right) {
    return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
  }

  Map<Integer, Integer> getStacksByDuration() {
    Map<Integer, Integer> result = new HashMap<>(stacksByDuration);
    pendingApplications.forEach((duration, stacks) -> merge(result, duration, stacks));
    return Map.copyOf(result);
  }

  @Override
  public int getValue() {
    int total = 0;
    for (int stacks : getStacksByDuration().values()) {
      total = sum(total, stacks);
    }
    return total;
  }

  /** Returns the longest remaining duration, or 0 if any poison is permanent. */
  @Override
  public int getDuration() {
    Map<Integer, Integer> groups = getStacksByDuration();
    return groups.containsKey(0)
        ? 0
        : groups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
  }

  /**
   * Retains the legacy mutation API for a single duration group. Multiple groups require an
   * explicit duration through CombatStatsComponent.applyStatusEffect instead.
   */
  @Override
  public void setValue(int value) {
    if (processing || stacksByDuration.size() != 1) {
      throw new IllegalStateException("Specify a poison duration using applyStatusEffect");
    }
    int duration = stacksByDuration.keySet().iterator().next();
    stacksByDuration.put(duration, Math.max(0, value));
  }

  @Override
  public void addValue(int amount) {
    long adjusted = (long) getValue() + amount;
    setValue((int) Math.max(0, Math.min(Integer.MAX_VALUE, adjusted)));
  }

  @Override
  public boolean tickAndCheckExpired() {
    if (processing) {
      throw new IllegalStateException("Poison is already being processed");
    }
    advanceDurations();
    return stacksByDuration.isEmpty();
  }

  private void advanceDurations() {
    Map<Integer, Integer> next = new HashMap<>();
    stacksByDuration.forEach(
        (duration, stacks) -> {
          if (duration == 0) {
            merge(next, 0, stacks);
          } else if (duration > 1) {
            merge(next, duration - 1, stacks);
          }
        });
    stacksByDuration.clear();
    stacksByDuration.putAll(next);
  }

  boolean processTick(IntConsumer applyDamage, BooleanSupplier stillAttached) {
    if (processing) {
      return false;
    }
    int damage = getValue();
    processing = true;
    try {
      if (damage > 0) {
        applyDamage.accept(damage);
      }
      if (!stillAttached.getAsBoolean()) {
        return false;
      }
      advanceDurations();
    } finally {
      processing = false;
      pendingApplications.forEach((duration, stacks) -> merge(stacksByDuration, duration, stacks));
      pendingApplications.clear();
    }
    return stacksByDuration.isEmpty();
  }
}
