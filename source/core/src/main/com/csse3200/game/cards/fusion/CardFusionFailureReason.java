package com.csse3200.game.cards.fusion;

/** Reason a card fusion request could not be completed. */
public enum CardFusionFailureReason {
  /** The fusion completed successfully. */
  NONE,

  /** This run has already completed its one permitted fusion. */
  FUSION_ALREADY_USED,

  /** The player did not select exactly three distinct owned card copies. */
  INVALID_SELECTION,

  /** At least one selected card copy is no longer in the player's deck. */
  CARD_NOT_IN_DECK,

  /** At least one selected card is not a Common card. */
  CARD_NOT_COMMON,

  /** The card library does not contain a Rare card to award. */
  NO_RARE_CARD_AVAILABLE
}
