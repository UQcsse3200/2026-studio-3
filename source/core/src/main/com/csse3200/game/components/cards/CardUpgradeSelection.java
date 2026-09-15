package com.csse3200.game.components.cards;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.*;

/** A class to gives information about the card upgrade selection when call */
public class CardUpgradeSelection {
  private final List<String> deck;
  private final CardService cardService;
  private final int maxSelections;
  private final List<UpgradeOption> options;
  private final LinkedHashSet<Integer> selected = new LinkedHashSet<>();
  private final Set<Integer> selectableIndices;

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
    Set<Integer> selectableIndicesCreate = new HashSet<>();

    for (UpgradeOption card : this.options) {
      selectableIndicesCreate.add(card.deckIndex());
    }
    this.selectableIndices = Set.copyOf(selectableIndicesCreate);
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
   * Return true if the deck index is selected and false if not
   *
   * @param deckIndex The current deck Index of the chosen card
   * @return boolean, true if the deck index is selected and false if not
   */
  public boolean isSelected(int deckIndex) {
    return this.selected.contains(deckIndex);
  }

  /**
   * Return the number of the remaining selectable card
   *
   * @return int Return the number of the remaining selectable card
   */
  public int remainingSelectable() {
    return this.maxSelections - selected.size();
  }

  /**
   * Return the list of upgrade options
   *
   * @return the upgrade options, one per upgradable card in the deck.
   */
  public List<UpgradeOption> getCardUpgradeOption() {
    return this.options;
  }

  /**
   * Return true if the selected card is at least one
   *
   * @return Return true if the selected card is at least one
   */
  public boolean canConfirm() {
    return !selected.isEmpty();
  }

  /**
   * Return true if the card can be selected
   *
   * @param deckIndex The current deck Index of the chosen card
   * @return Return true if the card is in the selectable list and is selected or the remaining
   *     selectable is more than 0
   */
  public boolean canSelect(int deckIndex) {
    return selectableIndices.contains(deckIndex)
        && (selected.contains(deckIndex) || (remainingSelectable() > 0));
  }

  /**
   * Flips the selection state of one upgradable card. An already selected card is unselected, which
   * is always allowed so the player can revise a choice after reaching the cap. An unselected card
   * is selected only while the cap has room; at the cap the call changes nothing.
   *
   * @param deckIndex The current deck Index of the chosen card
   * @return true if the card is selected after this call, false if it is not
   * @throws IllegalArgumentException if the deck index is not an upgradable option
   */
  public boolean toggle(int deckIndex) {
    if (!selectableIndices.contains(deckIndex)) {
      throw new IllegalArgumentException(
          "Deck index " + deckIndex + " is not an upgradable option");
    } else if (isSelected(deckIndex)) {
      selected.remove(deckIndex);
      return false;
    } else if (!canSelect(deckIndex)) {
      return false;
    } else {
      selected.add(deckIndex);
      return true;
    }
  }

  /**
   * Return the list of selected Deck Indices in sorted order
   *
   * @return Return the list of selected Deck Indices in sorted order
   */
  public List<Integer> getSelectedDeckIndices() {
    List<Integer> sortedList = new ArrayList<>(selected);
    sortedList.sort(Comparator.reverseOrder());
    return List.copyOf(sortedList);
  }

  /** Clear selected list */
  public void reset() {
    selected.clear();
  }
}
