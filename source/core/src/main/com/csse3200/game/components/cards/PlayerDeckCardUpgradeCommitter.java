package com.csse3200.game.components.cards;

import com.csse3200.game.cards.deck.PlayerDeck;
import java.util.List;
import java.util.Objects;

/** Applies upgrade selections to the persistent player deck by exact instance identity. */
public final class PlayerDeckCardUpgradeCommitter implements CardUpgradeCommitter {
  private final PlayerDeck playerDeck;

  public PlayerDeckCardUpgradeCommitter(PlayerDeck playerDeck) {
    this.playerDeck = Objects.requireNonNull(playerDeck, "playerDeck cannot be null");
  }

  @Override
  public void commitUpgrades(List<String> instanceIds) {
    if (instanceIds == null) {
      throw new IllegalArgumentException("instanceIds must not be null");
    }
    for (String instanceId : instanceIds) {
      playerDeck.upgradeCard(instanceId);
    }
  }
}
