package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug/cheat command: sets the player's current health to a specified amount. */
public class SetHealthCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SetHealthCommand.class);
  private final Entity player;

  public SetHealthCommand(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      logger.debug("Invalid arguments received for 'sethealth' command: {}", args);
      return false;
    }

    int amount;
    try {
      amount = Integer.parseInt(args.get(0));
    } catch (NumberFormatException e) {
      logger.debug("Non-numeric argument received for 'sethealth' command: {}", args);
      return false;
    }

    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      logger.warn("Player entity has no CombatStatsComponent; cannot set health");
      return false;
    }

    stats.setHealth(amount);
    return true;
  }
}
