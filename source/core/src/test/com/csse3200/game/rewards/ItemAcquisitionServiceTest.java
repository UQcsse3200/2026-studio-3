package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.maps.PlayerRunState;
import org.junit.jupiter.api.Test;

class ItemAcquisitionServiceTest {
  @Test
  void normalRewardsAndChanceEventsDiscoverGrantedItems() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    ItemAcquisitionService acquisitions = new ItemAcquisitionService(discovery);
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);

    acquisitions.grant(playerState, ItemType.ENERGY_CRYSTAL, ItemAcquisitionSource.NORMAL_REWARD);
    acquisitions.grant(playerState, ItemType.BLOOD_OATH_SIGIL, ItemAcquisitionSource.CHANCE_EVENT);

    assertEquals(1, playerState.getOwnedItemCount(ItemType.ENERGY_CRYSTAL));
    assertEquals(1, playerState.getOwnedItemCount(ItemType.BLOOD_OATH_SIGIL));
    assertTrue(discovery.isDiscovered(ItemType.ENERGY_CRYSTAL));
    assertTrue(discovery.isDiscovered(ItemType.BLOOD_OATH_SIGIL));
  }

  @Test
  void debugItemsAreAddedWithoutUnlockingTheLibraryEntry() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    ItemAcquisitionService acquisitions = new ItemAcquisitionService(discovery);
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);

    acquisitions.grant(playerState, ItemType.IRON_AEGIS, ItemAcquisitionSource.DEBUG);

    assertEquals(1, playerState.getOwnedItemCount(ItemType.IRON_AEGIS));
    assertFalse(discovery.isDiscovered(ItemType.IRON_AEGIS));
  }

  @Test
  void consumingAnItemDoesNotRelockItsLibraryEntry() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    ItemAcquisitionService acquisitions = new ItemAcquisitionService(discovery);
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);

    acquisitions.grant(playerState, ItemType.AMBROSIA_FLASK, ItemAcquisitionSource.NORMAL_REWARD);

    assertTrue(playerState.removeOwnedItem(ItemType.AMBROSIA_FLASK));
    assertFalse(playerState.hasOwnedItem(ItemType.AMBROSIA_FLASK));
    assertTrue(discovery.isDiscovered(ItemType.AMBROSIA_FLASK));
  }

  @Test
  void rejectsMissingGrantDetailsWithoutMutatingDiscovery() {
    ItemDiscoveryService discovery = new ItemDiscoveryService();
    ItemAcquisitionService acquisitions = new ItemAcquisitionService(discovery);
    PlayerRunState playerState = new PlayerRunState(100, 100, 0);

    assertThrows(
        NullPointerException.class,
        () -> acquisitions.grant(null, ItemType.IRON_AEGIS, ItemAcquisitionSource.NORMAL_REWARD));
    assertThrows(
        NullPointerException.class,
        () -> acquisitions.grant(playerState, null, ItemAcquisitionSource.NORMAL_REWARD));
    assertThrows(
        NullPointerException.class,
        () -> acquisitions.grant(playerState, ItemType.IRON_AEGIS, null));
    assertFalse(discovery.isDiscovered(ItemType.IRON_AEGIS));
  }
}
