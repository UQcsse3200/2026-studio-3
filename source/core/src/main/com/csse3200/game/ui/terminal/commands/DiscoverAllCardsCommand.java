package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardEntryView;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug/cheat command: reveals every card in the library, marking each as SEEN. Requested by Ray
 * (Team 6) to test card library UI without playing through a full run to encounter every card
 * naturally (shop/chance encounters are currently the only routes to discovery).
 */
public class DiscoverAllCardsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DiscoverAllCardsCommand.class);
  private final CardDiscoveryService discoveryService;

  public DiscoverAllCardsCommand(CardDiscoveryService discoveryService) {
    if (discoveryService == null) {
      throw new IllegalArgumentException("discoveryService must not be null");
    }
    this.discoveryService = discoveryService;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      logger.debug("Unexpected arguments received for 'discoverallcards' command: {}", args);
      return false;
    }

    List<String> cardIds =
        discoveryService.getEntries().stream().map(CardEntryView::cardId).toList();
    discoveryService.recordSeenAll(cardIds);
    logger.info("Revealed all {} cards in the library", cardIds.size());
    return true;
  }
}
