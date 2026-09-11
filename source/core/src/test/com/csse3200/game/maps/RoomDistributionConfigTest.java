package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoomDistributionConfigTest {

  @Test
  void storesConfiguredValues() {
    RoomDistributionConfig config = new RoomDistributionConfig(10, 60, 30, 10, 12345L);

    assertEquals(10, config.getNormalNodeCount());
    assertEquals(60, config.getCombatWeight());
    assertEquals(30, config.getEventWeight());
    assertEquals(10, config.getShopWeight());
    assertEquals(100, config.getTotalWeight());
    assertEquals(12345L, config.getSeed());
  }

  @Test
  void supportsMissingSeed() {
    RoomDistributionConfig config = new RoomDistributionConfig(10, 60, 30, 10);

    assertNull(config.getSeed());
  }

  @Test
  void rejectsInvalidNodeCount() {
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(0, 60, 30, 10));
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(-1, 60, 30, 10));
  }

  @Test
  void rejectsNegativeWeights() {
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(10, -1, 30, 10));
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(10, 60, -1, 10));
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(10, 60, 30, -1));
  }

  @Test
  void rejectsAllZeroWeights() {
    assertThrows(IllegalArgumentException.class, () -> new RoomDistributionConfig(10, 0, 0, 0));
  }

  @Test
  void preservesLargeWeightTotal() {
    RoomDistributionConfig config =
        new RoomDistributionConfig(3, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1L);

    assertEquals(6_442_450_941L, config.getTotalWeight());
  }
}
