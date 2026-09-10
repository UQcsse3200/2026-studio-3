package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MapGenerationConfigTest {

  @Test
  void storesConfiguredValues() {
    MapGenerationConfig config = new MapGenerationConfig(10, 60, 20, 10, 10, 12345L);

    assertEquals(10, config.getNormalNodeCount());
    assertEquals(60, config.getCombatWeight());
    assertEquals(20, config.getEventWeight());
    assertEquals(10, config.getEliteWeight());
    assertEquals(10, config.getShopWeight());
    assertEquals(100, config.getTotalWeight());
    assertEquals(12345L, config.getSeed());
  }

  @Test
  void rejectsInvalidNodeCount() {
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(0, 60, 20, 10, 10));
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(-1, 60, 20, 10, 10));
  }

  @Test
  void rejectsNegativeWeights() {
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(10, -1, 20, 10, 10));
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(10, 60, -1, 10, 10));
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(10, 60, 20, -1, 10));
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(10, 60, 20, 10, -1));
  }

  @Test
  void rejectsAllZeroWeights() {
    assertThrows(IllegalArgumentException.class, () -> new MapGenerationConfig(10, 0, 0, 0, 0));
  }

  @Test
  void preservesLargeWeightTotal() {
    MapGenerationConfig config =
        new MapGenerationConfig(3, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1L);

    assertEquals(6_442_450_941L, config.getTotalWeight());
  }
}
