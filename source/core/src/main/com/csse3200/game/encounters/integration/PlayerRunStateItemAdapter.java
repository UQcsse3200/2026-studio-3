package com.csse3200.game.encounters.integration;

import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Adapts Team 7's owned-item API to Chance transactions without running or reversing effects. */
public final class PlayerRunStateItemAdapter implements ItemRewardGateway {
  private final PlayerRunState playerState;
  private final List<PendingAddition> pendingAdditions = new ArrayList<>();

  private record PendingAddition(ItemType item, List<ItemType> before) {
    List<ItemType> after() {
      List<ItemType> items = new ArrayList<>(before);
      items.add(item);
      return items;
    }
  }

  /** Creates an ownership boundary for the current run's authoritative player state. */
  public PlayerRunStateItemAdapter(PlayerRunState playerState) {
    this.playerState = Objects.requireNonNull(playerState, "playerState cannot be null");
  }

  @Override
  public boolean containsItem(String itemId) {
    if (itemId == null) {
      return false;
    }
    try {
      ItemType.valueOf(itemId);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  @Override
  public synchronized boolean addItem(String itemId) {
    ItemType item = ItemType.valueOf(itemId);
    PendingAddition pending = new PendingAddition(item, List.copyOf(playerState.getOwnedItems()));
    pendingAdditions.add(pending);
    playerState.addOwnedItem(item);
    return playerState.getOwnedItems().equals(pending.after());
  }

  @Override
  public synchronized void commitItemAddition(String itemId) {
    int index = findLatestAddition(ItemType.valueOf(itemId));
    if (index >= 0) {
      pendingAdditions.remove(index);
    }
  }

  @Override
  public synchronized boolean rollbackItemAddition(String itemId) {
    int index = findLatestAddition(ItemType.valueOf(itemId));
    if (index < 0) {
      return false;
    }
    PendingAddition pending = pendingAdditions.get(index);
    List<ItemType> current = playerState.getOwnedItems();
    if (!current.equals(pending.before())) {
      if (!current.equals(pending.after())) {
        return false;
      }
      List<ItemType> afterRemoval = new ArrayList<>(current);
      afterRemoval.remove(pending.item());
      if (!playerState.removeOwnedItem(pending.item())
          || !playerState.getOwnedItems().equals(afterRemoval)) {
        return false;
      }
      // Team 7 removes the first equal enum copy. Preserve the old acquisition order when that
      // copy preceded other items; never overwrite a snapshot changed by another writer.
      if (!afterRemoval.equals(pending.before())) {
        playerState.restoreOwnedItems(pending.before());
      }
      if (!playerState.getOwnedItems().equals(pending.before())) {
        return false;
      }
    }
    pendingAdditions.remove(index);
    return true;
  }

  private int findLatestAddition(ItemType item) {
    for (int index = pendingAdditions.size() - 1; index >= 0; index--) {
      if (pendingAdditions.get(index).item() == item) {
        return index;
      }
    }
    return -1;
  }
}
