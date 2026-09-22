package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.rewards.ItemEffectApplier;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug/cheat command: gives the player a specified item's effect directly, without needing to win
 * it as a battle reward. Takes any ItemType enum name (e.g. "giveitem ENERGY_CRYSTAL"), so it
 * automatically supports new item types as they're added without needing this command updated.
 */
public class GiveItemCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GiveItemCommand.class);
  private final Entity player;

  public GiveItemCommand(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'giveitem' command: {}", args);
      return false;
    }

    ItemType itemType;
    try {
      itemType = ItemType.valueOf(args.get(0).toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.debug("Unknown item type received for 'giveitem' command: {}", args.get(0));
      return false;
    }

    ItemEffectApplier.applyItemEffect(itemType, player);
    return true;
  }
}
