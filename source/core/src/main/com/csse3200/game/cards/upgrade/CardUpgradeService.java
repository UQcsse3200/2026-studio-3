package com.csse3200.game.cards.upgrade;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core logic for the Card Upgrade system: determining which cards can be upgraded, and
 * executing an upgrade against a specific card instance in the player's deck.
 *
 * <p>Upgrades are tracked per card instance (not per card type), so upgrading one copy of a
 * card does not affect other copies of the same card in the deck. This relies on Team 5's deck
 * system providing a per-instance identifier for each card the player owns.
 */
public class CardUpgradeService {

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
     * Filters a list of card type IDs down to those that currently support upgrading.
     *
     * @param cardTypeIds card type IDs to check, e.g. distinct card types in the player's deck
     * @return the subset of IDs that can be upgraded; unknown IDs are silently skipped
     */
    public List<String> getUpgradableCardTypeIds(List<String> cardTypeIds) {
        List<String> result = new ArrayList<>();
        if (cardTypeIds == null) {
            return result;
        }
        for (String id : cardTypeIds) {
            Optional<CardConfig> configOpt = cardService.getCard(id);
            if (configOpt.isPresent() && canUpgrade(configOpt.get())) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Attempts to upgrade the specific card instance identified by {@code instanceId}.
     *
     * <p>NOTE: this method depends on Team 5's deck system to (a) resolve which card type a given
     * instance is, and (b) record the upgraded state against that specific instance. Both are
     * currently unconfirmed - see TODOs below.
     *
     * @param instanceId the unique identifier of the specific card instance in the player's deck
     * @return an {@link UpgradeResult} describing success or the reason for failure
     */
    public UpgradeResult upgradeCard(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return UpgradeResult.failure("Instance ID must not be null or blank");
        }

        // TODO: replace with Team 5's method once confirmed, e.g.:
        //     String cardTypeId = playerDeck.getCardTypeByInstanceId(instanceId);
        String cardTypeId = null; // placeholder until Team 5's API is available

        if (cardTypeId == null) {
            return UpgradeResult.failure("Unknown card instance: " + instanceId);
        }

        Optional<CardConfig> configOpt = cardService.getCard(cardTypeId);
        if (configOpt.isEmpty()) {
            return UpgradeResult.failure("Unknown card type: " + cardTypeId);
        }

        CardConfig config = configOpt.get();
        if (!canUpgrade(config)) {
            return UpgradeResult.failure("Card '" + cardTypeId + "' has no defined upgrade path");
        }

        // TODO: replace with Team 5's method once confirmed, e.g.:
        //     playerDeck.markCardAsUpgraded(instanceId);

        return UpgradeResult.success();
    }
}