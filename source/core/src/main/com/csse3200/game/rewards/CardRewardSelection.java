package com.csse3200.game.rewards;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable ordered card definitions offered by one post-battle Card reward. */
public record CardRewardSelection(List<String> cardIds) {
  public static final int MAX_CHOICES = 3;

  public CardRewardSelection {
    if (cardIds == null || cardIds.isEmpty()) {
      throw new IllegalArgumentException("cardIds must not be null or empty");
    }
    if (cardIds.size() > MAX_CHOICES) {
      throw new IllegalArgumentException("A card reward may contain at most 3 cards");
    }
    Set<String> uniqueIds = new HashSet<>();
    for (String cardId : cardIds) {
      if (cardId == null || cardId.isBlank()) {
        throw new IllegalArgumentException("cardIds must not contain null or blank IDs");
      }
      if (!uniqueIds.add(cardId)) {
        throw new IllegalArgumentException("Duplicate card reward ID: " + cardId);
      }
    }
    cardIds = List.copyOf(cardIds);
  }

  /** Returns whether this fixed offer contains the selected definition ID. */
  public boolean contains(String cardId) {
    return cardId != null && cardIds.contains(cardId);
  }
}
