package com.csse3200.game.encounters.results;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Converts committed Event outcomes into read-only display data, without applying them again. */
public final class ChanceAcquisitionResults {
  private ChanceAcquisitionResults() {}

  public static Optional<AcquisitionResult> fromApplied(
      ChanceResolution resolution, String resultId, CardService cards) {
    return fromApplied(resolution, resultId, cards, "");
  }

  /** The choice description is authoritative display copy for any granted item's effects. */
  public static Optional<AcquisitionResult> fromApplied(
      ChanceResolution resolution, String resultId, CardService cards, String choiceDescription) {
    if (resolution == null || !resolution.isSuccess() || resolution.getOutcome() == null) {
      return Optional.empty();
    }
    List<AcquisitionResult.Entry> entries = new ArrayList<>();
    appendChange(
        entries, "health", "Health", resolution.getHealthBefore(), resolution.getHealthAfter());
    appendChange(
        entries, "gold", "Gold", resolution.getCurrencyBefore(), resolution.getCurrencyAfter());

    Map<String, Integer> cardCounts = new LinkedHashMap<>();
    resolution.getOutcome().getCardRewardIds().forEach(id -> cardCounts.merge(id, 1, Integer::sum));
    cardCounts.forEach(
        (id, count) -> {
          Optional<CardConfig> card = cards == null ? Optional.empty() : cards.getCard(id);
          if (card == null) {
            card = Optional.empty();
          }
          String name = card.map(config -> config.name).orElse(id);
          String description = card.map(config -> config.description).orElse("Added to your deck.");
          entries.add(
              new AcquisitionResult.Entry(
                  AcquisitionResult.Kind.CARD, id, count, name, description, null));
        });
    Map<String, Integer> itemCounts = new LinkedHashMap<>();
    resolution.getOutcome().getItemRewardIds().forEach(id -> itemCounts.merge(id, 1, Integer::sum));
    itemCounts.forEach(
        (id, count) ->
            entries.add(
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.ITEM,
                    id,
                    count,
                    itemName(id),
                    choiceDescription == null || choiceDescription.isBlank()
                        ? "Added to your inventory."
                        : choiceDescription,
                    null)));
    return entries.isEmpty()
        ? Optional.empty()
        : Optional.of(new AcquisitionResult(resultId, entries));
  }

  private static String itemName(String id) {
    try {
      return ItemFormatting.formatItemName(ItemType.valueOf(id));
    } catch (IllegalArgumentException exception) {
      StringBuilder name = new StringBuilder();
      for (String word : id.split("_")) {
        if (!name.isEmpty()) {
          name.append(' ');
        }
        name.append(word.charAt(0)).append(word.substring(1).toLowerCase());
      }
      return name.toString();
    }
  }

  private static void appendChange(
      List<AcquisitionResult.Entry> entries, String id, String name, int before, int after) {
    long change = (long) after - before;
    if (change == 0L) {
      return;
    }
    if (Math.abs(change) > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Confirmed result magnitude exceeds display range");
    }
    boolean positive = change > 0L;
    int quantity = (int) Math.abs(change);
    entries.add(
        new AcquisitionResult.Entry(
            positive
                ? AcquisitionResult.Kind.POSITIVE_EFFECT
                : AcquisitionResult.Kind.NEGATIVE_EFFECT,
            id,
            quantity,
            name + (positive ? " gained" : " lost"),
            name + (positive ? " increased by " : " decreased by ") + quantity + ".",
            null));
  }
}
