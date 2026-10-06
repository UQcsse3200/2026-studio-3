package com.csse3200.game.rewards;

import java.util.ArrayList;
import java.util.List;

/** Stores the items collected by a player during a run. */
public class ItemInventory {
  private final List<ItemType> items = new ArrayList<>();

  /** Adds one copy of an item while preserving acquisition order. */
  public void addItem(ItemType itemType) {
    requireItem(itemType);
    items.add(itemType);
  }

  /**
   * Removes one copy of an item.
   *
   * @return true when a copy was removed, or false when the item was not owned
   */
  public boolean removeItem(ItemType itemType) {
    requireItem(itemType);
    return items.remove(itemType);
  }

  /** Returns whether at least one copy of the item is owned. */
  public boolean hasItem(ItemType itemType) {
    requireItem(itemType);
    return items.contains(itemType);
  }

  /** Returns the number of copies owned for an item. */
  public int getItemCount(ItemType itemType) {
    requireItem(itemType);
    return (int) items.stream().filter(item -> item == itemType).count();
  }

  /** Returns an immutable snapshot of all items in acquisition order. */
  public List<ItemType> getItems() {
    return List.copyOf(items);
  }

  public int size() {
    return items.size();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  private void requireItem(ItemType itemType) {
    if (itemType == null) {
      throw new IllegalArgumentException("itemType must not be null");
    }
  }
}
