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

/** Maps a committed Chance resolution to display data without granting or applying anything. */
public final class ChanceAcquisitionResults {
  private ChanceAcquisitionResults() {}

  /** Maps only successful resolutions, omitting zero resource changes and empty results. */
  public static Optional<AcquisitionResult> fromApplied(
      ChanceResolution resolution, String resultId, CardService cards) {
    return fromApplied(resolution, resultId, cards, "");
  }

  /**
   * Maps display amounts and acquired goods. Positive healing advertises its configured amount,
   * while actual health remains capped in the committed resolution. Choice copy is a fallback for
   * other item descriptions; Curse descriptions explain their authoritative future lifecycle rather
   * than immediate effects.
   *
   * @param resolution committed snapshot; unsuccessful or absent resolutions produce no result
   * @param resultId stable ID retained by the owning session
   * @param cards card metadata service, or null when unavailable
   * @param choiceDescription optional display copy for items without a specific description
   * @return immutable display data, or empty when there were no committed changes
   */
  public static Optional<AcquisitionResult> fromApplied(
      ChanceResolution resolution, String resultId, CardService cards, String choiceDescription) {
    if (resolution == null || !resolution.isSuccess() || resolution.getOutcome() == null) {
      return Optional.empty();
    }
    List<AcquisitionResult.Entry> entries = new ArrayList<>();
    int gold = Math.subtractExact(resolution.getCurrencyAfter(), resolution.getCurrencyBefore());
    int hp = Math.subtractExact(resolution.getHealthAfter(), resolution.getHealthBefore());
    // Healing copy advertises the configured amount; settlement still uses the real HP cap.
    if (resolution.getOutcome().getHealthDelta() > 0) {
      hp = resolution.getOutcome().getHealthDelta();
    }
    if (gold != 0) entries.add(AcquisitionResult.Entry.gold(gold));
    if (hp != 0) entries.add(AcquisitionResult.Entry.hp(hp));
    counts(resolution.getOutcome().getCardRewardIds())
        .forEach(
            (id, count) -> {
              Optional<CardConfig> card =
                  Optional.ofNullable(cards)
                      .map(service -> service.getCard(id))
                      .orElseGet(Optional::empty);
              entries.add(
                  new AcquisitionResult.Entry(
                      AcquisitionResult.Kind.CARD,
                      id,
                      count,
                      textOr(card.map(config -> config.name).orElse(null), id),
                      textOr(
                          card.map(config -> config.description).orElse(null),
                          "Added to your deck."),
                      null));
            });
    counts(resolution.getOutcome().getItemRewardIds())
        .forEach(
            (id, count) ->
                entries.add(
                    new AcquisitionResult.Entry(
                        AcquisitionResult.Kind.ITEM,
                        id,
                        count,
                        itemName(id),
                        itemDescription(id, choiceDescription),
                        null)));
    return entries.isEmpty()
        ? Optional.empty()
        : Optional.of(new AcquisitionResult(resultId, entries));
  }

  private static Map<String, Integer> counts(List<String> ids) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    ids.forEach(id -> counts.merge(id, 1, Math::addExact));
    return counts;
  }

  private static String textOr(String text, String fallback) {
    return text == null || text.isBlank() ? fallback : text;
  }

  private static String itemName(String id) {
    try {
      return ItemFormatting.formatItemName(ItemType.valueOf(id));
    } catch (IllegalArgumentException exception) {
      // Names of unmerged Curse definitions are display metadata, not replacement item effects.
      return switch (id) {
        case "BRAND_OF_THE_DAMNED" -> "Brand of the Damned";
        case "BLOOD_OATH_SIGIL" -> "Blood-Oath Sigil";
        case "SHATTERED_AEGIS" -> "Shattered Aegis";
        case "ASHEN_EMBER" -> "Ashen Ember";
        default -> id;
      };
    }
  }

  private static String itemDescription(String id, String fallback) {
    return switch (id) {
      case "BRAND_OF_THE_DAMNED" ->
          "In future battles, every Strength gain is reduced by 1 per copy (minimum 0).";
      case "BLOOD_OATH_SIGIL" ->
          "In future battles, lose 2 HP per copy at the start of each player turn, bypassing Block and Armour (minimum 1 HP).";
      case "SHATTERED_AEGIS" ->
          "In future battles, every Block gain is reduced by 1 per copy (minimum 0).";
      case "ASHEN_EMBER" ->
          "When the player is recreated, maximum Energy is reduced by 1 per copy (minimum 1).";
      default -> textOr(fallback, "Added to your inventory. The Item system manages its effects.");
    };
  }
}
