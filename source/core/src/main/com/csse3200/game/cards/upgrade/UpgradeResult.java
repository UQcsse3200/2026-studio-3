package com.csse3200.game.cards.upgrade;

/** Result of attempting to upgrade a card, returned by {@link CardUpgradeService#upgradeCard}. */
public final class UpgradeResult {

    private final boolean success;
    private final String failureReason;

    private UpgradeResult(boolean success, String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    public static UpgradeResult success() {
        return new UpgradeResult(true, null);
    }

    public static UpgradeResult failure(String reason) {
        return new UpgradeResult(false, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    /** Returns the failure reason, or null if this result represents a success. */
    public String getFailureReason() {
        return failureReason;
    }
}