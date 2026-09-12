package com.csse3200.game.cards.upgrade;

/** Reason a card upgrade request could not be completed. */
public enum UpgradeFailureReason {
    /** The card was upgraded successfully. */
    NONE,

    /** The base card ID was null or blank. */
    BLANK_CARD_ID,

    /** Team 6's card service does not contain the requested base card ID. */
    UNKNOWN_CARD,

    /** The requested card has no defined upgrade path (upgradedEffects is null or empty). */
    NO_UPGRADE_PATH,

    /** The upgraded card definition has not been registered in Team 6's card library yet. */
    UPGRADED_DEFINITION_MISSING
}