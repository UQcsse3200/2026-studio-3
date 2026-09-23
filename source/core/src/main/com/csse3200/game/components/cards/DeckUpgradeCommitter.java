package com.csse3200.game.components.cards;

import com.csse3200.game.cards.deck.PlayerDeck;
import java.util.List;

public class DeckUpgradeCommitter implements CardUpgradeCommitter {
  private final PlayerDeck deck;

  /**
   * throw IAE when playerDeck is null
   *
   * @throws IllegalArgumentException when player deck is null
   */
  public DeckUpgradeCommitter(PlayerDeck deck) {
    if (deck == null) {
      throw new IllegalArgumentException("PlayerDeck cannot be null");
    }
    this.deck = deck;
  }

  @Override
  public void commitUpgrades(List<String> instanceIds) {
    for (String instanceId : instanceIds) {
      deck.upgradeCard(instanceId);
    }
  }
}
