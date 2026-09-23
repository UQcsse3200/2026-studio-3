package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemInventoryTest {
  @Test
  void storesStackedItemsInAcquisitionOrder() {
    ItemInventory inventory = new ItemInventory();

    inventory.addItem(ItemType.LUCKY_COIN);
    inventory.addItem(ItemType.ENERGY_CRYSTAL);
    inventory.addItem(ItemType.LUCKY_COIN);

    assertEquals(
        List.of(ItemType.LUCKY_COIN, ItemType.ENERGY_CRYSTAL, ItemType.LUCKY_COIN),
        inventory.getItems());
    assertEquals(2, inventory.getItemCount(ItemType.LUCKY_COIN));
    assertEquals(3, inventory.size());
    assertTrue(inventory.hasItem(ItemType.ENERGY_CRYSTAL));
  }

  @Test
  void removesOnlyOneCopyAtATime() {
    ItemInventory inventory = new ItemInventory();
    inventory.addItem(ItemType.MERCHANTS_FAVOR);
    inventory.addItem(ItemType.MERCHANTS_FAVOR);

    assertTrue(inventory.removeItem(ItemType.MERCHANTS_FAVOR));
    assertEquals(1, inventory.getItemCount(ItemType.MERCHANTS_FAVOR));
    assertTrue(inventory.hasItem(ItemType.MERCHANTS_FAVOR));

    assertTrue(inventory.removeItem(ItemType.MERCHANTS_FAVOR));
    assertFalse(inventory.hasItem(ItemType.MERCHANTS_FAVOR));
    assertFalse(inventory.removeItem(ItemType.MERCHANTS_FAVOR));
    assertTrue(inventory.isEmpty());
  }

  @Test
  void returnsAnImmutableSnapshot() {
    ItemInventory inventory = new ItemInventory();
    inventory.addItem(ItemType.LUCKY_COIN);

    List<ItemType> snapshot = inventory.getItems();
    inventory.addItem(ItemType.ENERGY_CRYSTAL);

    assertEquals(List.of(ItemType.LUCKY_COIN), snapshot);
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(ItemType.ENERGY_CRYSTAL));
  }

  @Test
  void rejectsNullItems() {
    ItemInventory inventory = new ItemInventory();

    assertThrows(IllegalArgumentException.class, () -> inventory.addItem(null));
    assertThrows(IllegalArgumentException.class, () -> inventory.removeItem(null));
    assertThrows(IllegalArgumentException.class, () -> inventory.hasItem(null));
    assertThrows(IllegalArgumentException.class, () -> inventory.getItemCount(null));
  }
}
