package com.csse3200.game.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChanceEncounterSelectorTest {
  @Test
  void shouldSelectUsingConfiguredWeights() {
    ChanceEncounter first = createEncounter("first", 1);
    ChanceEncounter second = createEncounter("second", 2);
    ChanceEncounter third = createEncounter("third", 3);
    ChanceEncounterSelector selector =
        new ChanceEncounterSelector(
            List.of(first, second, third), new SequenceRandom(0, 1, 2, 3, 4, 5));

    assertSame(first, selector.select());
    assertSame(second, selector.select());
    assertSame(second, selector.select());
    assertSame(third, selector.select());
    assertSame(third, selector.select());
    assertSame(third, selector.select());
  }

  @Test
  void shouldUseCorrectSelectionBoundaries() {
    ChanceEncounter first = createEncounter("first", 2);
    ChanceEncounter second = createEncounter("second", 3);

    assertSame(
        first, new ChanceEncounterSelector(List.of(first, second), new SequenceRandom(1)).select());
    assertSame(
        second,
        new ChanceEncounterSelector(List.of(first, second), new SequenceRandom(2)).select());
  }

  @Test
  void shouldReproduceSequenceWithSameSeed() {
    List<ChanceEncounter> encounters =
        List.of(
            createEncounter("first", 1), createEncounter("second", 2), createEncounter("third", 3));

    assertEquals(
        selectIds(encounters, new Random(148L), 30), selectIds(encounters, new Random(148L), 30));
  }

  @Test
  void shouldAllowDifferentSeedsToProduceDifferentSequences() {
    List<ChanceEncounter> encounters =
        List.of(
            createEncounter("first", 1), createEncounter("second", 2), createEncounter("third", 3));

    assertNotEquals(
        selectIds(encounters, new Random(148L), 30), selectIds(encounters, new Random(149L), 30));
  }

  @Test
  void shouldAlwaysSelectOnlyEncounter() {
    ChanceEncounter only = createEncounter("only", 17);
    ChanceEncounterSelector selector = new ChanceEncounterSelector(List.of(only), new Random(148L));

    for (int i = 0; i < 20; i++) {
      assertSame(only, selector.select());
    }
  }

  @Test
  void shouldDefensivelyCopyEncounterPool() {
    ChanceEncounter only = createEncounter("only", 1);
    List<ChanceEncounter> encounters = new ArrayList<>();
    encounters.add(only);
    ChanceEncounterSelector selector = new ChanceEncounterSelector(encounters, new Random(148L));

    encounters.clear();

    assertSame(only, selector.select());
    assertEquals(0, encounters.size());
  }

  @Test
  void shouldRejectPoolWhoseTotalWeightExceedsIntegerRange() {
    List<ChanceEncounter> encounters =
        List.of(createEncounter("maximum", Integer.MAX_VALUE), createEncounter("overflow", 1));

    assertThrows(
        IllegalArgumentException.class,
        () -> new ChanceEncounterSelector(encounters, new Random(148L)));
  }

  private static List<String> selectIds(
      List<ChanceEncounter> encounters, Random random, int selectionCount) {
    ChanceEncounterSelector selector = new ChanceEncounterSelector(encounters, random);
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < selectionCount; i++) {
      ids.add(selector.select().getId());
    }
    return ids;
  }

  private static ChanceEncounter createEncounter(String id, int weight) {
    ChanceOutcome noEffect = new ChanceOutcome(0, 0);
    ChanceChoice choice = new ChanceChoice("continue", "Continue.", noEffect);
    return new ChanceEncounter(id, id, List.of(choice), weight);
  }

  private static final class SequenceRandom extends Random {
    private static final long serialVersionUID = 1L;

    private final int[] values;
    private int index;

    private SequenceRandom(int... values) {
      this.values = values.clone();
    }

    @Override
    public int nextInt(int bound) {
      int value = values[index++];
      if (value < 0 || value >= bound) {
        throw new IllegalArgumentException("Test value is outside random bound " + bound);
      }
      return value;
    }
  }
}
