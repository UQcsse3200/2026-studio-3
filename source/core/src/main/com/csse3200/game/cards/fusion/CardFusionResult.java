package com.csse3200.game.cards.fusion;

import java.util.Optional;

/** Immutable outcome of a request to fuse three Common card copies. */
public record CardFusionResult(
    boolean successful, Optional<String> rewardedCardId, CardFusionFailureReason failureReason) {
  public CardFusionResult {
    if (rewardedCardId == null) {
      throw new IllegalArgumentException("rewardedCardId must not be null");
    }
    if (failureReason == null) {
      throw new IllegalArgumentException("failureReason must not be null");
    }
    if (successful != rewardedCardId.isPresent()) {
      throw new IllegalArgumentException("Only successful fusions may include a rewarded card");
    }
    if (successful != (failureReason == CardFusionFailureReason.NONE)) {
      throw new IllegalArgumentException("Successful fusions must use the NONE failure reason");
    }
  }

  /** Creates a successful fusion result. */
  public static CardFusionResult success(String rewardedCardId) {
    if (rewardedCardId == null || rewardedCardId.isBlank()) {
      throw new IllegalArgumentException("rewardedCardId must not be null or blank");
    }
    return new CardFusionResult(
        true, Optional.of(rewardedCardId), CardFusionFailureReason.NONE);
  }

  /** Creates an unsuccessful fusion result without changing the player's deck. */
  public static CardFusionResult failure(CardFusionFailureReason failureReason) {
    if (failureReason == null || failureReason == CardFusionFailureReason.NONE) {
      throw new IllegalArgumentException("A failed fusion requires a failure reason");
    }
    return new CardFusionResult(false, Optional.empty(), failureReason);
  }
}
