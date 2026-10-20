package com.csse3200.game.shop;

import java.util.List;
import java.util.OptionalInt;

/** Immutable UI data. A stock revision must accompany purchase/refresh commands. */
public record ShopSnapshot(
    long revision,
    Integer gold,
    float effectiveDiscount,
    int successfulRefreshes,
    OptionalInt nextRefreshCost,
    boolean completed,
    List<Offer> offers) {
  public ShopSnapshot {
    offers = List.copyOf(offers);
  }

  public record Offer(
      String id,
      String cardId,
      String name,
      String description,
      int basePrice,
      int price,
      int stock) {}
}
