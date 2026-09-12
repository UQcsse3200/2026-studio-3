package com.csse3200.game.cards.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CardUpgradeServiceTest {

    /** Simple in-memory stub implementing CardService for testing, no JSON loading required. */
    private static class StubCardService implements CardService {
        private final Map<String, CardConfig> cards = new HashMap<>();

        void add(CardConfig config) {
            cards.put(config.id, config);
        }

        @Override
        public Optional<CardConfig> getCard(String cardId) {
            return Optional.ofNullable(cards.get(cardId));
        }

        @Override
        public List<CardConfig> getAllCards() {
            return List.copyOf(cards.values());
        }
    }

    private CardConfig buildCard(String id, boolean upgradable) {
        CardConfig config = new CardConfig();
        config.id = id;
        config.name = id;
        config.description = "test card";
        config.cost = 1;
        config.type = CardType.ATTACK;
        config.rarity = Rarity.COMMON;
        config.target = TargetType.SINGLE_ENEMY;
        config.effects = new EffectConfig[] { new EffectConfig(EffectType.DAMAGE, 4) };
        config.texturePath = "path.png";
        if (upgradable) {
            config.upgradedEffects = new EffectConfig[] { new EffectConfig(EffectType.DAMAGE, 6) };
        }
        return config;
    }

    @Test
    void shouldReportUpgradableCard() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("poison_dagger", true));
        CardUpgradeService service = new CardUpgradeService(cardService);

        boolean result = service.canUpgrade(cardService.getCard("poison_dagger").get());
        assertTrue(result);
    }

    @Test
    void shouldReportNonUpgradableCard() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("strike", false));
        CardUpgradeService service = new CardUpgradeService(cardService);

        boolean result = service.canUpgrade(cardService.getCard("strike").get());
        assertFalse(result);
    }

    @Test
    void shouldReportFalseForNullConfig() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        assertFalse(service.canUpgrade(null));
    }

    @Test
    void shouldFilterUpgradableCardIds() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("strike", false));
        cardService.add(buildCard("poison_dagger", true));
        CardUpgradeService service = new CardUpgradeService(cardService);

        List<String> result = service.getUpgradableCardIds(List.of("strike", "poison_dagger"));

        assertEquals(1, result.size());
        assertEquals("poison_dagger", result.get(0));
    }

    @Test
    void shouldSkipUnknownCardIds() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("poison_dagger", true));
        CardUpgradeService service = new CardUpgradeService(cardService);

        List<String> result = service.getUpgradableCardIds(List.of("poison_dagger", "unknown_card"));

        assertEquals(1, result.size());
        assertEquals("poison_dagger", result.get(0));
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        List<String> result = service.getUpgradableCardIds(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGenerateUpgradedCardIdWithSuffix() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        assertEquals("strike_upgraded", service.getUpgradedCardId("strike"));
    }

    @Test
    void shouldThrowForNullBaseCardIdInGetUpgradedCardId() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        assertThrows(IllegalArgumentException.class, () -> service.getUpgradedCardId(null));
    }

    @Test
    void shouldThrowForBlankBaseCardIdInGetUpgradedCardId() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        assertThrows(IllegalArgumentException.class, () -> service.getUpgradedCardId("  "));
    }

    @Test
    void shouldFailUpgradeForBlankCardId() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        UpgradeResult result = service.upgradeCard("");
        assertFalse(result.isSuccess());
        assertEquals(UpgradeFailureReason.BLANK_CARD_ID, result.getFailureReason());
    }

    @Test
    void shouldFailUpgradeForUnknownBaseCard() {
        CardUpgradeService service = new CardUpgradeService(new StubCardService());
        UpgradeResult result = service.upgradeCard("does_not_exist");
        assertFalse(result.isSuccess());
        assertEquals(UpgradeFailureReason.UNKNOWN_CARD, result.getFailureReason());
    }

    @Test
    void shouldFailUpgradeForCardWithoutUpgradePath() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("strike", false));
        CardUpgradeService service = new CardUpgradeService(cardService);

        UpgradeResult result = service.upgradeCard("strike");
        assertFalse(result.isSuccess());
        assertEquals(UpgradeFailureReason.NO_UPGRADE_PATH, result.getFailureReason());
    }

    @Test
    void shouldFailUpgradeWhenUpgradedDefinitionNotRegistered() {
        // "strike" supports upgrading, but "strike_upgraded" itself has not been registered yet -
        // this documents the current gap pending Team 6's confirmation of how upgraded card
        // definitions come to exist in the CardLibrary.
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("strike", true));
        CardUpgradeService service = new CardUpgradeService(cardService);

        UpgradeResult result = service.upgradeCard("strike");
        assertFalse(result.isSuccess());
        assertEquals(UpgradeFailureReason.UPGRADED_DEFINITION_MISSING, result.getFailureReason());
    }

    @Test
    void shouldSucceedWhenUpgradedDefinitionExists() {
        StubCardService cardService = new StubCardService();
        cardService.add(buildCard("strike", true));
        cardService.add(buildCard("strike_upgraded", false)); // the upgraded definition itself
        CardUpgradeService service = new CardUpgradeService(cardService);

        UpgradeResult result = service.upgradeCard("strike");

        assertTrue(result.isSuccess());
        assertEquals("strike_upgraded", result.getUpgradedCardId());
        assertEquals(UpgradeFailureReason.NONE, result.getFailureReason());
    }
}