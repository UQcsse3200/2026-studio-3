package com.csse3200.game.components.cards;

import java.util.List;

public interface CardUpgradeCommitter {
  void commitUpgrades(List<String> instanceIds);
}
