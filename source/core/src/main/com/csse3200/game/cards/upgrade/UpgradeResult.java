package com.csse3200.game.cards.upgrade;

/** Result of attempting to upgrade a card, returned by {@link CardUpgradeService#upgradeCard}. */
public final class UpgradeResult {

    private final boolean success;
    private final String upgradedCardId;
    private final String failureReason;

    private UpgradeResult(boolean success, String upgradedCardId, String failureReason) {
        this.success = success;
        this.upgradedCardId = upgradedCardId;
        this.failureReason = failureReason;
    }

    /**
     * Creates a successful result carrying the new upgraded card ID.
     *
     * @param upgradedCardId the ID the caller should swap into the player's deck in place of
     *     the original card ID
     */
    public static UpgradeResult success(String upgradedCardId) {
        return new UpgradeResult(true, upgradedCardId, null);
    }

    public static UpgradeResult failure(String reason) {
        return new UpgradeResult(false, null, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the new card ID to swap into the deck in place of the original.
     * Null if this result is a failure.
     */
    public String getUpgradedCardId() {
        return upgradedCardId;
    }

    /** Returns the failure reason, or null if this result represents a success. */
    public String getFailureReason() {
        return failureReason;
    }
}