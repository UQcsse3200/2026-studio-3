package com.csse3200.game.cards.upgrade;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core logic for the Card Upgrade system: determining which cards can be upgraded, and
 * resolving the upgraded card ID for a given base card.
 *
 * <p>Upgrading does not mutate any existing card. It resolves a separate, independent
 * "upgraded" card definition (e.g. "strike" -> "strike_upgraded") for the caller to swap into
 * the player's deck, so upgrading one copy of a card never affects other copies of the same
 * base card.
 */
public class CardUpgradeService {

    private static final String UPGRADED_SUFFIX = "_upgraded";

    private final CardService cardService;

    public CardUpgradeService(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Returns true if the given card definition has a defined upgrade path.
     *
     * @param config card definition to check, may be null
     * @return true if the card can be upgraded
     */
    public boolean canUpgrade(CardConfig config) {
        return config != null
                && config.upgradedEffects != null
                && config.upgradedEffects.length > 0;
    }

    /**
     * Filters a list of card IDs down to those that currently support upgrading.
     *
     * @param cardIds card IDs to check, e.g. the player's current deck
     * @return the subset of IDs that can be upgraded; unknown IDs are silently skipped
     */
    public List<String> getUpgradableCardIds(List<String> cardIds) {
        List<String> result = new ArrayList<>();
        if (cardIds == null) {
            return result;
        }
        for (String id : cardIds) {
            Optional<CardConfig> configOpt = cardService.getCard(id);
            if (configOpt.isPresent() && canUpgrade(configOpt.get())) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Returns the ID of the upgraded variant of a given base card ID, following our naming
     * convention. This is a pure naming rule and does not check whether that card definition
     * actually exists yet - see {@link #upgradeCard} for the existence check.
     *
     * @param baseCardId the original card's ID, e.g. "strike"; must not be null or blank
     * @return the upgraded card's ID, e.g. "strike_upgraded"
     * @throws IllegalArgumentException if baseCardId is null or blank
     */
    public String getUpgradedCardId(String baseCardId) {
        if (baseCardId == null || baseCardId.isBlank()) {
            throw new IllegalArgumentException("baseCardId must not be null or blank");
        }
        return baseCardId + UPGRADED_SUFFIX;
    }

    /**
     * Checks whether a base card ID is fully eligible for upgrading, combining all conditions
     * required for a successful upgrade: the card must exist, must have a defined upgrade path,
     * and its upgraded variant must already be registered. Consolidated here so callers don't
     * have to re-assemble these checks themselves.
     *
     * @param baseCardId the card ID to check
     * @return a failed {@link UpgradeResult} if any condition is not met, or a successful one
     *     carrying the resolved upgraded card ID if all conditions pass
     */
    private UpgradeResult checkUpgradeEligibility(String baseCardId) {
        if (baseCardId == null || baseCardId.isBlank()) {
            return UpgradeResult.failure(UpgradeFailureReason.BLANK_CARD_ID);
        }

        Optional<CardConfig> configOpt = cardService.getCard(baseCardId);
        if (configOpt.isEmpty()) {
            return UpgradeResult.failure(UpgradeFailureReason.UNKNOWN_CARD);
        }

        if (!canUpgrade(configOpt.get())) {
            return UpgradeResult.failure(UpgradeFailureReason.NO_UPGRADE_PATH);
        }

        String upgradedCardId = getUpgradedCardId(baseCardId);
        Optional<CardConfig> upgradedConfigOpt = cardService.getCard(upgradedCardId);
        if (upgradedConfigOpt.isEmpty()) {
            return UpgradeResult.failure(UpgradeFailureReason.UPGRADED_DEFINITION_MISSING);
        }

        return UpgradeResult.success(upgradedCardId);
    }

    /**
     * Attempts to upgrade a card, identified by its base card ID, to its upgraded variant.
     *
     * <p>Upgrading does NOT modify the original card definition or mutate anything in place.
     * Instead, it looks up a separate, independent "upgraded" card definition (e.g. "strike" ->
     * "strike_upgraded") and returns that new ID for the caller to swap into the player's deck.
     * This means upgrading one copy of a card never affects any other copies of the same base
     * card the player owns, since each copy is just an ID in the deck, not a distinct object.
     *
     * <p>This method does NOT touch Team 5's PlayerDeck directly. It only validates the upgrade
     * request and resolves the correct upgraded card ID. The actual deck mutation (removing
     * {@code baseCardId} and adding the returned upgraded ID) is the caller's responsibility,
     * since PlayerDeck ownership belongs to Team 5.
     *
     * @param baseCardId the card ID to upgrade, e.g. "strike"
     * @return an {@link UpgradeResult}; on success, {@link UpgradeResult#getUpgradedCardId()}
     *     contains the new card ID that should replace {@code baseCardId} in the deck
     */
    public UpgradeResult upgradeCard(String baseCardId) {
        return checkUpgradeEligibility(baseCardId);
    }
}