package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug/cheat command: gives the player a specified amount of gold. */
public class GiveGoldCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GiveGoldCommand.class);
  private final Entity player;

  public GiveGoldCommand(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'givegold' command: {}", args);
      return false;
    }

    int amount;
    try {
      amount = Integer.parseInt(args.get(0));
    } catch (NumberFormatException e) {
      logger.debug("Non-numeric argument received for 'givegold' command: {}", args);
      return false;
    }

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.warn("Player entity has no InventoryComponent; cannot give gold");
      return false;
    }

    inventory.addGold(amount);
    return true;
  }
}
