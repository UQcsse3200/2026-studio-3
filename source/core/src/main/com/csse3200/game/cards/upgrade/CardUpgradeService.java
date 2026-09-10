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
     * @param baseCardId the original card's ID, e.g. "strike"
     * @return the upgraded card's ID, e.g. "strike_upgraded"
     */
    public String getUpgradedCardId(String baseCardId) {
        return baseCardId + UPGRADED_SUFFIX;
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
        // Step 1: reject obviously invalid input before doing any lookups.
        if (baseCardId == null || baseCardId.isBlank()) {
            return UpgradeResult.failure("Card ID must not be null or blank");
        }

        // Step 2: confirm the base card actually exists in Team 6's card library.
        Optional<CardConfig> configOpt = cardService.getCard(baseCardId);
        if (configOpt.isEmpty()) {
            return UpgradeResult.failure("Unknown card ID: " + baseCardId);
        }

        // Step 3: confirm this specific card has an upgrade path defined at all.
        // A card with upgradedEffects == null was never designed to be upgradeable.
        CardConfig config = configOpt.get();
        if (!canUpgrade(config)) {
            return UpgradeResult.failure("Card '" + baseCardId + "' has no defined upgrade path");
        }

        // Step 4: work out what the upgraded card's ID should be, using our naming convention.
        String upgradedCardId = getUpgradedCardId(baseCardId);

        // Step 5: confirm the upgraded card definition actually exists and is registered.
        // TODO: how this definition comes to exist is still being confirmed with Team 6 -
        // either they hand-write a "strike_upgraded" entry in cards.json, or we dynamically
        // build and register one from upgradedEffects at runtime. Until that's settled, this
        // lookup will fail for any card whose upgraded variant hasn't been registered yet.
        Optional<CardConfig> upgradedConfigOpt = cardService.getCard(upgradedCardId);
        if (upgradedConfigOpt.isEmpty()) {
            return UpgradeResult.failure("Upgraded card definition not found: " + upgradedCardId);
        }

        // Step 6: everything checks out - hand back the new card ID for the caller to swap
        // into PlayerDeck (remove baseCardId, add upgradedCardId).
        return UpgradeResult.success(upgradedCardId);
    }
}