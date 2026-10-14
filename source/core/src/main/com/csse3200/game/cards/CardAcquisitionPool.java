package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable policy describing which registered card definitions normal acquisition systems may
 * offer.
 *
 * <p>The pool is shared eligibility policy, not a consumable draw pile. Shop, event and post-battle
 * generators may sample it independently; displaying or acquiring a card never removes that card
 * from this object.
 */
public final class CardAcquisitionPool {
  private final CardService cardService;
  private final List<String> eligibleCardIds;
  private final Set<String> eligibleCardIdSet;

  /** Creates and validates a stable pool ordered by card ID. */
  public CardAcquisitionPool(CardService cardService, Collection<String> eligibleCardIds) {
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
    if (eligibleCardIds == null) {
      throw new IllegalArgumentException("eligibleCardIds must not be null");
    }

    Set<String> validatedIds = new HashSet<>();
    for (String cardId : eligibleCardIds) {
      if (cardId == null || cardId.isBlank()) {
        throw new IllegalArgumentException("eligible card IDs must not be null or blank");
      }
      if (!validatedIds.add(cardId)) {
        throw new IllegalArgumentException("Duplicate eligible card ID: " + cardId);
      }
      if (cardService.getCard(cardId).isEmpty()) {
        throw new IllegalArgumentException("Unknown eligible card ID: " + cardId);
      }
    }

    List<String> stableIds = new ArrayList<>(validatedIds);
    stableIds.sort(String::compareTo);
    this.eligibleCardIds = List.copyOf(stableIds);
    this.eligibleCardIdSet = Set.copyOf(validatedIds);
  }

  /** Returns whether the definition may be offered through normal acquisition. */
  public boolean isEligible(String cardId) {
    return cardId != null && eligibleCardIdSet.contains(cardId);
  }

  /** Returns an immutable, stably ordered snapshot of eligible definition IDs. */
  public List<String> eligibleCardIds() {
    return eligibleCardIds;
  }

  /** Returns eligible definitions in the same stable order as {@link #eligibleCardIds()}. */
  public List<CardConfig> getEligibleCards() {
    return eligibleCardIds.stream()
        .map(cardService::getCard)
        .map(java.util.Optional::orElseThrow)
        .toList();
  }
}
