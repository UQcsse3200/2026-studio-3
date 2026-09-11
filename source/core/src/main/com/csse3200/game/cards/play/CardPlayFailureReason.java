package com.csse3200.game.cards.play;

/** Reason a card play request could not be completed. */
public enum CardPlayFailureReason {
  /** The card was played successfully. */
  NONE,

  /** Team 6's card service does not contain the requested card ID. */
  UNKNOWN_CARD,

  /** The retrieved Team 6 card config does not satisfy the card validation contract. */
  INVALID_CARD_CONFIG,

  /** The selected target does not match the target required by the card config. */
  INVALID_TARGET,

  /** The requested card is not currently in the battle deck hand. */
  CARD_NOT_IN_HAND,

  /** Team 7's energy component rejected the card cost. */
  NOT_ENOUGH_ENERGY,

  /** Effect resolution or the local deck commit failed after validation. */
  RESOLUTION_FAILED,

  /**
   * @deprecated use {@link #INVALID_CARD_CONFIG}.
   */
  @Deprecated
  INVALID_CARD,

  /**
   * @deprecated use {@link #NOT_ENOUGH_ENERGY}.
   */
  @Deprecated
  INSUFFICIENT_ENERGY
}
