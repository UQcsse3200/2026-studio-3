package com.csse3200.game.shop;

/** Immutable receipt. Replaying the same action returns the same receipt without charging again. */
public record ShopRefreshResult(Status status, int chargedGold, long stockRevision) {
  public enum Status {
    SUCCESS,
    NOT_CONFIGURED,
    COST_OVERFLOW,
    INSUFFICIENT_GOLD,
    INVALID_REQUEST,
    BUSY,
    STALE_STOCK,
    SHOP_CLOSED,
    GENERATION_FAILED,
    PAYMENT_FAILED,
    ROLLBACK_FAILED
  }

  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }
}
