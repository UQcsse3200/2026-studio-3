package com.csse3200.game.rewards;

import com.csse3200.game.maps.PlayerRunState;
import java.util.Objects;

/**
 * The single entry point for adding an item to a run when its acquisition source is known.
 *
 * <p>Keeping the source here prevents UI previews and debug commands from accidentally unlocking
 * the Item Library, while allowing all real acquisition flows to share the same discovery rule.
 */
public class ItemAcquisitionService {
  private final ItemDiscoveryService discoveryService;

  public ItemAcquisitionService(ItemDiscoveryService discoveryService) {
    this.discoveryService =
        Objects.requireNonNull(discoveryService, "discoveryService must not be null");
  }

  /**
   * Adds an item to the active run and records permanent discovery only for legitimate sources.
   *
   * @param playerState persistent state of the active run
   * @param item item to add
   * @param source reason the item was granted
   */
  public void grant(PlayerRunState playerState, ItemType item, ItemAcquisitionSource source) {
    Objects.requireNonNull(playerState, "playerState must not be null");
    Objects.requireNonNull(item, "item must not be null");
    Objects.requireNonNull(source, "source must not be null");

    playerState.addOwnedItem(item);
    if (source.discoversItem()) {
      discoveryService.recordDiscovered(item);
    }
  }
}
