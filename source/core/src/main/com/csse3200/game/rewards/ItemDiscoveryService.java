package com.csse3200.game.rewards;

import com.csse3200.game.events.EventHandler;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns permanent item discovery progress independently of the run-scoped item inventory. */
public class ItemDiscoveryService {
  /** Fired with the changed item whenever an entry advances to a new unlock state. */
  public static final String ENTRY_UPDATED_EVENT = "entryUpdated";

  private static final Logger logger = LoggerFactory.getLogger(ItemDiscoveryService.class);

  private final Map<ItemType, ItemUnlockState> progress = new EnumMap<>(ItemType.class);
  private final EventHandler events = new EventHandler();

  /** Creates a discovery collection with every currently configured item locked. */
  public ItemDiscoveryService() {
    for (ItemType item : ItemType.values()) {
      progress.put(item, ItemUnlockState.LOCKED);
    }
  }

  /** Returns the event handler used to notify presentation code of progress changes. */
  public EventHandler getEvents() {
    return events;
  }

  /** Returns whether the player has permanently discovered an item. */
  public boolean isDiscovered(ItemType item) {
    return item != null && progress.get(item).isAtLeast(ItemUnlockState.DISCOVERED);
  }

  /** Returns a stable, immutable copy of the current progress for every item. */
  public Map<ItemType, ItemUnlockState> getProgressSnapshot() {
    return Collections.unmodifiableMap(new EnumMap<>(progress));
  }

  /**
   * Atomically replaces discovery progress. Entries omitted from the input become locked.
   *
   * <p>This supports an explicit profile reset. Save-game restoration should use {@link
   * #mergeProgress(Map)} instead so a stale save cannot hide a discovery made later.
   */
  public void replaceProgress(Map<ItemType, ItemUnlockState> restoredProgress) {
    validateProgress(restoredProgress, "restored progress");

    Map<ItemType, ItemUnlockState> next = lockedProgress();
    next.putAll(restoredProgress);
    applyProgress(next);
  }

  /**
   * Merges discovery progress without ever downgrading an already discovered item.
   *
   * @param incoming progress to merge into this profile
   */
  public void mergeProgress(Map<ItemType, ItemUnlockState> incoming) {
    validateProgress(incoming, "incoming progress");

    Map<ItemType, ItemUnlockState> next = new EnumMap<>(progress);
    for (Map.Entry<ItemType, ItemUnlockState> entry : incoming.entrySet()) {
      if (!next.get(entry.getKey()).isAtLeast(entry.getValue())) {
        next.put(entry.getKey(), entry.getValue());
      }
    }
    applyProgress(next);
  }

  /**
   * Records that an item was legitimately obtained.
   *
   * @param item discovered item
   * @return true only when this call advances the item's state
   */
  public boolean recordDiscovered(ItemType item) {
    if (item == null) {
      logger.debug("Ignoring discovery for a null item");
      return false;
    }
    if (isDiscovered(item)) {
      return false;
    }

    progress.put(item, ItemUnlockState.DISCOVERED);
    events.trigger(ENTRY_UPDATED_EVENT, item);
    return true;
  }

  private static Map<ItemType, ItemUnlockState> lockedProgress() {
    Map<ItemType, ItemUnlockState> locked = new EnumMap<>(ItemType.class);
    for (ItemType item : ItemType.values()) {
      locked.put(item, ItemUnlockState.LOCKED);
    }
    return locked;
  }

  private static void validateProgress(
      Map<ItemType, ItemUnlockState> candidate, String description) {
    if (candidate == null) {
      throw new IllegalArgumentException(description + " must not be null");
    }
    for (Map.Entry<ItemType, ItemUnlockState> entry : candidate.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        throw new IllegalArgumentException(description + " contains an invalid entry");
      }
    }
  }

  private void applyProgress(Map<ItemType, ItemUnlockState> next) {
    Map<ItemType, ItemUnlockState> previous = new EnumMap<>(progress);
    progress.clear();
    progress.putAll(next);
    for (ItemType item : ItemType.values()) {
      if (previous.get(item) != next.get(item)) {
        events.trigger(ENTRY_UPDATED_EVENT, item);
      }
    }
  }
}
