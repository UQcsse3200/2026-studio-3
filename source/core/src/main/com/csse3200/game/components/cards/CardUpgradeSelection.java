package com.csse3200.game.components.cards;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A class to gives information about the card upgrade selection when call */
public class CardUpgradeSelection {
  private final List<String> deck;
  private final CardService cardService;
  private final int maxSelections;
  private final List<UpgradeOption> options;

  /**
   * A constructor for Card upgrade selection
   *
   * @param deck receive the list of the player's deck
   * @param cardService call card service to receive card deck
   * @param maxSelections the max selections for the upgrade
   * @throws IllegalArgumentException Deck being null
   * @throws IllegalArgumentException CardService being null
   * @throws IllegalArgumentException Max Selections being less than 1
   */
  public CardUpgradeSelection(List<String> deck, CardService cardService, int maxSelections) {
    if (deck == null) {
      throw new IllegalArgumentException("Deck cannot be null");
    }
    if (cardService == null) {
      throw new IllegalArgumentException("CardService cannot be null");
    }
    if (maxSelections < 1) {
      throw new IllegalArgumentException("Max Selections cannot be less than 1");
    }
    this.deck = List.copyOf(deck);
    this.cardService = cardService;
    this.maxSelections = maxSelections;
    this.options = buildUpgradeOptions();
  }

  /**
   * Blueprint for the upgrade option
   *
   * @param deckIndex for keeping the index of the cards
   * @param current look at the current card
   * @param preview look at the card's upgraded
   */
  public record UpgradeOption(int deckIndex, ResolvedCard current, ResolvedCard preview) {}

  private List<UpgradeOption> buildUpgradeOptions() {
    ArrayList<UpgradeOption> copies = new ArrayList<>();
    CardResolver resolver = new CardResolver();
    for (int index = 0; index < deck.size(); index++) {
      String cardId = deck.get(index);
      Optional<CardConfig> found = cardService.getCard(cardId);

      if (found.isEmpty() || found.get().upgrade == null) {
        continue;
      }
      CardConfig cardConfig = found.get();
      CardInstance cardInstance =
          new CardInstance("preview-" + index, cardId, CardInstance.BASE_LEVEL);
      ResolvedCard baseCard = resolver.resolve(cardConfig, cardInstance);
      ResolvedCard upgradedCard = resolver.resolve(cardConfig, cardInstance.upgrade());

      copies.add(new UpgradeOption(index, baseCard, upgradedCard));
    }

    return List.copyOf(copies);
  }

  /**
   * Return the list of upgrade options
   *
   * @return List<UpgradeOption> List of upgrade options
   */
  public List<UpgradeOption> getCardUpgradeOption() {
    return buildUpgradeOptions();
  }
}
