package com.csse3200.game.rewards;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;

/** Shared helpers for curse items that reduce the player's Strength and Block gains. */
public final class CursePenalties {
  public static final String GAIN_REDUCED_EVENT = "curseGainReduced";
  public static final String STRENGTH_PENALTY = "STRENGTH_PENALTY";
  public static final String BLOCK_PENALTY = "BLOCK_PENALTY";
  public static final String BLOOD_OATH = "BLOOD_OATH";
  public static final int BLOOD_OATH_DAMAGE_PER_COPY = 2;

  /** Actual result of a cursed gain, for short-lived battle feedback. */
  public record GainReduction(String penalty, int requested, int actual) {}

  private CursePenalties() {}

  /** Adds one stack of the given penalty, lasting for the rest of the battle. */
  public static void addPenalty(CombatStatsComponent stats, String penalty) {
    StatusEffect existing = stats.getStatusEffect(penalty);
    if (existing == null) {
      stats.applyStatusEffect(penalty, 1, 0);
    } else {
      existing.addValue(1);
    }
  }

  /** Returns the gain left after subtracting the penalty, never below 0. */
  public static int reduce(CombatStatsComponent stats, String penalty, int amount) {
    StatusEffect existing = stats.getStatusEffect(penalty);
    int value = existing == null ? 0 : existing.getValue();
    int actual = Math.max(0, amount - value);
    if (actual < amount && stats.getEntity() != null) {
      stats
          .getEntity()
          .getEvents()
          .trigger(GAIN_REDUCED_EVENT, new GainReduction(penalty, amount, actual));
    }
    return actual;
  }

  /** Returns the number of stacks of the given penalty, or 0 if none. */
  public static int getPenalty(CombatStatsComponent stats, String penalty) {
    StatusEffect existing = stats.getStatusEffect(penalty);
    return existing == null ? 0 : existing.getValue();
  }
}
