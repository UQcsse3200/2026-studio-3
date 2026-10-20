package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemDiscoveryServiceTest {
  @Test
  void startsWithEveryConfiguredItemLocked() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();

    assertEquals(ItemType.values().length, discovery.getProgressSnapshot().size());
    assertTrue(
        discovery.getProgressSnapshot().values().stream()
            .allMatch(state -> state == ItemUnlockState.LOCKED));
  }

  @Test
  void recordsDiscoveryOnlyOnce() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();

    assertTrue(discovery.recordDiscovered(ItemType.ENERGY_CRYSTAL));
    assertFalse(discovery.recordDiscovered(ItemType.ENERGY_CRYSTAL));
    assertFalse(discovery.recordDiscovered(null));
    assertTrue(discovery.isDiscovered(ItemType.ENERGY_CRYSTAL));
  }

  @Test
  void mergesProgressWithoutDowngradingExistingDiscoveries() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    discovery.recordDiscovered(ItemType.IRON_AEGIS);

    discovery.mergeProgress(
        Map.of(
            ItemType.IRON_AEGIS, ItemUnlockState.LOCKED,
            ItemType.WARRIORS_CREST, ItemUnlockState.DISCOVERED));

    assertTrue(discovery.isDiscovered(ItemType.IRON_AEGIS));
    assertTrue(discovery.isDiscovered(ItemType.WARRIORS_CREST));
  }

  @Test
  void replacesProgressForExplicitProfileResetAndExposesImmutableSnapshot() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    discovery.recordDiscovered(ItemType.LUCKY_COIN);

    discovery.replaceProgress(Map.of(ItemType.MERCHANTS_FAVOR, ItemUnlockState.DISCOVERED));

    assertFalse(discovery.isDiscovered(ItemType.LUCKY_COIN));
    assertTrue(discovery.isDiscovered(ItemType.MERCHANTS_FAVOR));
    assertThrows(
        UnsupportedOperationException.class,
        () -> discovery.getProgressSnapshot().put(ItemType.LUCKY_COIN, ItemUnlockState.DISCOVERED));
  }
}
