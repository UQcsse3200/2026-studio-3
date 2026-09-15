package com.csse3200.game.chance;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Selects Chance Encounters using their configured relative weights. */
public final class ChanceEncounterSelector {
  private final List<ChanceEncounter> encounters;
  private final Random random;
  private final int totalWeight;

  /**
   * Creates a deterministic-capable weighted selector.
   *
   * <p>The encounter list is defensively copied. The supplied random source is used directly so
   * callers and tests can control selection reproducibly.
   *
   * @param encounters non-empty encounters with positive weights
   * @param random random source used for each selection
   * @throws NullPointerException if the list, an encounter, or the random source is null
   * @throws IllegalArgumentException if the pool is empty or its total weight exceeds the supported
   *     integer range
   */
  public ChanceEncounterSelector(List<ChanceEncounter> encounters, Random random) {
    this.encounters = List.copyOf(Objects.requireNonNull(encounters, "encounters"));
    this.random = Objects.requireNonNull(random, "random");

    if (this.encounters.isEmpty()) {
      throw new IllegalArgumentException("Chance Encounter selection pool must not be empty");
    }

    long accumulatedWeight = 0L;
    for (ChanceEncounter encounter : this.encounters) {
      accumulatedWeight += encounter.getWeight();
      if (accumulatedWeight > Integer.MAX_VALUE) {
        throw new IllegalArgumentException(
            "Chance Encounter selection pool total weight must not exceed " + Integer.MAX_VALUE);
      }
    }
    this.totalWeight = (int) accumulatedWeight;
  }

  /**
   * Selects one encounter according to the pool's relative weights.
   *
   * @return selected encounter
   */
  public ChanceEncounter select() {
    int selection = random.nextInt(totalWeight);
    long cumulativeWeight = 0L;

    for (ChanceEncounter encounter : encounters) {
      cumulativeWeight += encounter.getWeight();
      if (selection < cumulativeWeight) {
        return encounter;
      }
    }

    throw new IllegalStateException("Unable to select from a validated Chance Encounter pool");
  }
}
