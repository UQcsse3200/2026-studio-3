package com.csse3200.game.cards.upgrade;

/** Result of attempting to upgrade a card, returned by {@link CardUpgradeService#upgradeCard}. */
public final class UpgradeResult {

    private final boolean success;
    private final String upgradedCardId;
    private final UpgradeFailureReason failureReason;

    private UpgradeResult(boolean success, String upgradedCardId, UpgradeFailureReason failureReason) {
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
        return new UpgradeResult(true, upgradedCardId, UpgradeFailureReason.NONE);
    }



    /**
     * Creates a failed result with the given reason.
     *
     * @param reason why the upgrade could not be completed
     */
    public static UpgradeResult failure(UpgradeFailureReason reason) {
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

    /** Returns the failure reason. {@link UpgradeFailureReason#NONE} if this result is a success. */
    public UpgradeFailureReason getFailureReason() {
        return failureReason;
    }
}