package com.csse3200.game.components.cards;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Selects exact owned card copies for upgrade and builds resolved before/after previews. */
public class CardUpgradeSelection {
  private final Supplier<List<CardInstance>> deckSource;
  private final int maxSelections;
  private final CardService cardService;
  private List<UpgradeOption> options;
  private Set<String> selectableInstanceIds;
  private final Set<String> selected = new LinkedHashSet<>();

  public CardUpgradeSelection(List<CardInstance> deck, CardService cardService, int maxSelections) {
    this(fixedDeckSource(deck), cardService, maxSelections);
  }

  private CardUpgradeSelection(
      Supplier<List<CardInstance>> deckSource, CardService cardService, int maxSelections) {
    this.deckSource = deckSource;
    if (cardService == null) {
      throw new IllegalArgumentException("CardService cannot be null");
    }
    if (maxSelections < 1) {
      throw new IllegalArgumentException("Max Selections cannot be less than 1");
    }
    this.cardService = cardService;
    this.maxSelections = maxSelections;
    refresh();
  }

  /** Creates a selection model that can refresh from the current persistent deck after commits. */
  public static CardUpgradeSelection forPlayerDeck(
      PlayerDeck playerDeck, CardService cardService, int maxSelections) {
    if (playerDeck == null) {
      throw new IllegalArgumentException("PlayerDeck cannot be null");
    }
    return new CardUpgradeSelection(playerDeck::getCards, cardService, maxSelections);
  }

  /** Rebuilds eligible options from the latest deck state and clears the previous selection. */
  public void refresh() {
    List<CardInstance> deck = deckSource.get();
    if (deck == null) {
      throw new IllegalStateException("Deck source returned null");
    }
    this.options = buildUpgradeOptions(List.copyOf(deck));
    Set<String> selectableIds = new HashSet<>();
    for (UpgradeOption card : options) {
      selectableIds.add(card.instance().instanceId());
    }
    this.selectableInstanceIds = Set.copyOf(selectableIds);
    selected.clear();
  }

  /** One upgrade choice tied to the exact persistent card copy it will replace. */
  public record UpgradeOption(CardInstance instance, ResolvedCard current, ResolvedCard preview) {}

  private List<UpgradeOption> buildUpgradeOptions(List<CardInstance> deck) {
    List<UpgradeOption> copies = new ArrayList<>();
    CardResolver resolver = new CardResolver();
    for (CardInstance instance : deck) {
      Optional<CardConfig> found = cardService.getCard(instance.cardId());
      if (found.isEmpty() || found.get().upgrade == null || instance.isUpgraded()) {
        continue;
      }
      CardConfig config = found.get();
      copies.add(
          new UpgradeOption(
              instance,
              resolver.resolve(config, instance),
              resolver.resolve(config, instance.upgrade())));
    }
    return List.copyOf(copies);
  }

  public boolean isSelected(String instanceId) {
    return selected.contains(instanceId);
  }

  public int remainingSelectable() {
    return maxSelections - selected.size();
  }

  public List<UpgradeOption> getCardUpgradeOption() {
    return options;
  }

  public boolean canConfirm() {
    return !selected.isEmpty();
  }

  public boolean canSelect(String instanceId) {
    return selectableInstanceIds.contains(instanceId)
        && (selected.contains(instanceId) || remainingSelectable() > 0);
  }

  /** Toggles one exact upgradable copy without selecting its same-definition siblings. */
  public boolean toggle(String instanceId) {
    if (!selectableInstanceIds.contains(instanceId)) {
      throw new IllegalArgumentException(
          "Instance ID " + instanceId + " is not an upgradable option");
    }
    if (selected.remove(instanceId)) {
      return false;
    }
    if (!canSelect(instanceId)) {
      return false;
    }
    selected.add(instanceId);
    return true;
  }

  /** Returns selected instance IDs in selection order. */
  public List<String> getSelectedInstanceIds() {
    return List.copyOf(selected);
  }

  public void reset() {
    selected.clear();
  }

  private static Supplier<List<CardInstance>> fixedDeckSource(List<CardInstance> deck) {
    if (deck == null) {
      throw new IllegalArgumentException("Deck cannot be null");
    }
    List<CardInstance> snapshot = List.copyOf(deck);
    return () -> snapshot;
  }
}
