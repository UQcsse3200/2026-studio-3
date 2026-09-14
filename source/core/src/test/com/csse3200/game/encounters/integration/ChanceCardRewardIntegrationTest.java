package com.csse3200.game.encounters.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.encounters.integration.mocks.MockPlayerStateGateway;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceCardRewardIntegrationTest {
  @Test
  void shouldRollbackExactRewardAdditionWithoutRemovingExistingDuplicate() {
    CardService cardService = TestCardService.withCards("bandage", "strike");
    PlayerDeck playerDeck = new PlayerDeck(cardService, List.of("bandage", "strike", "bandage"));
    MockPlayerStateGateway player = new MockPlayerStateGateway(100, 40);
    player.failNextCurrencyUpdate();
    ChanceOutcomeApplier applier =
        new ChanceOutcomeApplier(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));

    ChanceResolution result = applier.apply(new ChanceOutcome(-10, 5, "bandage"));

    assertEquals(ChanceResolution.Status.PLAYER_UPDATE_FAILED, result.getStatus());
    assertEquals(List.of("bandage", "strike", "bandage"), playerDeck.getCardIds());
    assertEquals(100, player.getHealth());
    assertEquals(40, player.getCurrency());
  }

  @Test
  void shouldPersistCombinedRewardThroughSharedRunStateDeck() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    RunState runState = new RunState();
    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardService);
    int initialBandageCount = playerDeck.count("bandage");
    PlayerRunState playerState = new PlayerRunState(80, 100, 40);
    Entity playerEntity =
        new Entity()
            .addComponent(new CombatStatsComponent(1, 10, 1))
            .addComponent(new InventoryComponent(0));
    playerState.applyTo(playerEntity);
    ComponentPlayerStateAdapter player =
        new ComponentPlayerStateAdapter(
            playerEntity.getComponent(CombatStatsComponent.class),
            playerEntity.getComponent(InventoryComponent.class));
    ChanceOutcomeApplier applier =
        new ChanceOutcomeApplier(
            player, new CardServiceCatalogAdapter(cardService), new PlayerDeckAdapter(playerDeck));

    ChanceResolution result = applier.apply(new ChanceOutcome(-10, 15, "bandage"));
    playerState.captureFrom(playerEntity);
    PlayerDeck reenteredDeck = runState.getOrCreatePlayerDeck(cardService);

    assertTrue(result.isSuccess());
    assertSame(playerDeck, reenteredDeck);
    assertEquals(initialBandageCount + 1, reenteredDeck.count("bandage"));
    assertEquals(70, playerState.getCurrentHealth());
    assertEquals(55, playerState.getGold());
  }
}
