package com.csse3200.game.components.cards;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCardUpgradeCommitter implements CardUpgradeCommitter {
  private static final Logger logger = LoggerFactory.getLogger(LoggingCardUpgradeCommitter.class);

  @Override
  public void commitUpgrades(List<String> instanceIds) {
    logger.info("Upgrade requested for instance ID {}", instanceIds);
  }
}
