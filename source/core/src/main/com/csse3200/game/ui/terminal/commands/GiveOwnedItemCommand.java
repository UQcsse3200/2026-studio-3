package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rewards.ItemAcquisitionService;
import com.csse3200.game.rewards.ItemAcquisitionSource;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug command that adds an item to the run inventory so its inventory UI can be previewed. */
public class GiveOwnedItemCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GiveOwnedItemCommand.class);

  private final PlayerRunState playerState;
  private final ItemAcquisitionService itemAcquisitionService;

  public GiveOwnedItemCommand(
      PlayerRunState playerState, ItemAcquisitionService itemAcquisitionService) {
    if (playerState == null) {
      throw new IllegalArgumentException("playerState must not be null");
    }
    if (itemAcquisitionService == null) {
      throw new IllegalArgumentException("itemAcquisitionService must not be null");
    }
    this.playerState = playerState;
    this.itemAcquisitionService = itemAcquisitionService;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'giveowneditem' command: {}", args);
      return false;
    }

    try {
      itemAcquisitionService.grant(
          playerState,
          ItemType.valueOf(args.getFirst().toUpperCase()),
          ItemAcquisitionSource.DEBUG);
      return true;
    } catch (IllegalArgumentException exception) {
      logger.debug("Unknown item type received for 'giveowneditem' command: {}", args.getFirst());
      return false;
    }
  }
}
