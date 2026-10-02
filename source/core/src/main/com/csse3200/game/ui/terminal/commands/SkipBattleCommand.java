package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.combat.BattleController;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debug/cheat command: instantly wins the current battle by defeating every enemy. */
public class SkipBattleCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SkipBattleCommand.class);
  private final BattleController controller;

  public SkipBattleCommand(BattleController controller) {
    if (controller == null) {
      throw new IllegalArgumentException("controller must not be null");
    }
    this.controller = controller;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      logger.debug("Unexpected arguments received for 'skipbattle' command: {}", args);
      return false;
    }
    controller.forceEnemiesDefeated();
    return true;
  }
}
