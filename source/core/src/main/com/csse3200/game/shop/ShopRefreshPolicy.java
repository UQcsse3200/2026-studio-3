package com.csse3200.game.shop;

import java.util.OptionalInt;

/** First refresh is free; subsequent fees double from the team's configured first paid fee. */
public record ShopRefreshPolicy(Integer firstPaidFee) {
  public static final int DEFAULT_FIRST_PAID_FEE = 2;

  public ShopRefreshPolicy {
    if (firstPaidFee != null && firstPaidFee <= 0) {
      throw new IllegalArgumentException("First paid refresh fee must be positive");
    }
  }

  /** Approved map-Shop fees: free, then 2, 4, 8, 16 gold and so on. */
  public static ShopRefreshPolicy standard() {
    return new ShopRefreshPolicy(DEFAULT_FIRST_PAID_FEE);
  }

  /** Allows the free refresh without inventing a price for the unconfirmed paid refreshes. */
  public static ShopRefreshPolicy pendingFee() {
    return new ShopRefreshPolicy(null);
  }

  public OptionalInt costAfter(int successfulRefreshes) {
    if (successfulRefreshes < 0) {
      throw new IllegalArgumentException("Refresh count must not be negative");
    }
    if (successfulRefreshes == 0) {
      return OptionalInt.of(0);
    }
    if (firstPaidFee == null || successfulRefreshes > 31) {
      return OptionalInt.empty();
    }
    long cost = (long) firstPaidFee << (successfulRefreshes - 1);
    return cost > Integer.MAX_VALUE ? OptionalInt.empty() : OptionalInt.of((int) cost);
  }
}
